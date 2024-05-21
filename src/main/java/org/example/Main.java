package org.example;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.example.db.DBQueries;
import org.example.db.DBUtils;
import org.example.db.LanguageTranslation;
import org.example.query.Match;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.example.Converters.*;
import static org.example.db.DBQueries.initTables;

public class Main {
    public static void main(String[] args) {
        MustacheFactory mf = new DefaultMustacheFactory();

        initTables();

        System.out.println(">> Serving Server");

        var app = Javalin.create(config -> {
                    config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                        mapper.enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature());
                    }));
                })
                .get("/", ctx -> {
                    System.out.println(">> Serving Index");

                    try (var stream = Files.lines(Paths.get(Objects.requireNonNull(Main.class.getResource("../../index.html")).toURI()))) {
                        String foo = stream.collect(Collectors.joining("\n"));
                        ctx.html(foo);
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                })
                .get("/menu-hud/search", ctx -> {
                    String searchLike = ctx.queryParam("searchLike");
                    System.out.println(">> searchLike: " + searchLike);

                    var matches = DBUtils.execute(DBQueries.menuHUDSearch(searchLike))
                            .stream()
                            .map(languageTranslationWithMetaFromResults)
                            .map(matchFromLanguageTranslationWithMeta)
                            .collect(Collectors.toCollection(ArrayList::new));

                    var nonMatches = DBUtils.execute(
                                    DBQueries.menuHUDReverseSearch(
                                            matches.stream()
                                                    .map(Match::assetId)
                                                    .collect(Collectors.toCollection(ArrayList::new))))
                            .stream()
                            .map(languageTranslationWithMetaFromResults)
                            .map(matchFromLanguageTranslationWithMeta)
                            .collect(Collectors.toCollection(ArrayList::new));

                    var queryResultsExecutedTemplate = generateQueryResultsTemplate(matches, searchLike, nonMatches, mf);

                    ctx.html(queryResultsExecutedTemplate);
                })
                .get("/menu-hud", ctx -> {
                    System.out.println(">> Serving Menu/HUD");

                    var matches = DBUtils.execute(DBQueries.LANGUAGE_TRANSLATIONS_WITH_META)
                            .stream()
                            .map(languageTranslationWithMetaFromResults)
                            .map(matchFromLanguageTranslationWithMeta)
                            .collect(Collectors.toCollection(ArrayList::new));

                    var nonMatches = DBUtils.execute(
                                    DBQueries.menuHUDReverseSearch(
                                            matches.stream()
                                                    .map(Match::assetId)
                                                    .collect(Collectors.toCollection(ArrayList::new))))
                            .stream()
                            .map(languageTranslationWithMetaFromResults)
                            .map(matchFromLanguageTranslationWithMeta)
                            .collect(Collectors.toCollection(ArrayList::new));

                    var searchExecutedTemplate = generateSearchTemplate(mf);
                    var queryResultsExecutedTemplate = generateQueryResultsTemplate(matches, null, nonMatches, mf);

                    ctx.html(searchExecutedTemplate + queryResultsExecutedTemplate);
                })
                .get("/inspect/asset/{assetId}", ctx -> {
                    System.out.println(">> Responding to inspect asset");

                    var assetId = Integer.parseInt(ctx.pathParam("assetId"));

                    System.out.println(">> got assetID : " + assetId);

                    var generatedHtml = getInspectAssetHtml(assetId, mf);

                    ctx.html(generatedHtml);
                })
                .get("/assets/mutate", ctx -> {
                    System.out.println(">> Responding to mutate asset");

                    var title = ctx.queryParam("title");
                    var url = ctx.queryParam("url");
                    var description = ctx.queryParam("description");
                    var assetId = Integer.parseInt(ctx.queryParam("assetId"));

                    // update asset
                    System.out.println(">> updating asset");
                    DBUtils.execute(DBQueries.updateAsset(title, url, description, assetId));
                    // fetch the newly updated asset to display
                    System.out.println(">> asset updated, displaying newly updated asset");
                    var generatedHtml = getInspectAssetHtml(assetId, mf);

                    ctx.html(generatedHtml);
                })
                .get("/inspect/asset/translations/{assetId}", ctx -> {
                    System.out.println(">> Serving asset's translations");
                    var assetId = Integer.parseInt(ctx.pathParam("assetId"));
                    System.out.println(">> got assetId " + assetId);
                    var generatedHtml = generateAssetTranslationsHtml(assetId, mf);
                    ctx.html(generatedHtml);
                })
                .post("/translations/mutate", ctx -> {
                    System.out.println(">> Responding to mutate translation");
                    var mutations = ctx.bodyAsClass(MutateTranslationRequest.class);
                    System.out.println("mutations: " + mutations);

                    ArrayList<LanguageTranslation> mutating = DBUtils.execute(DBQueries.getLanguageTranslationsForAsset(mutations.assetId()))
                            .stream()
                            .map(languageTranslationFromObjectArray)
                            // get all translations that are different from original
                            .filter(translationBeforeMutation -> {
                                String translationToMutate;
                                switch (translationBeforeMutation.languageName()) {
                                    case "english" -> translationToMutate = mutations.englishTranslation();
                                    case "german" -> translationToMutate = mutations.germanTranslation();
                                    default ->
                                            throw new RuntimeException("Unsupported language: " + translationBeforeMutation.languageName());
                                }
                                return !Objects.equals(translationToMutate, translationBeforeMutation.translation());
                            })
                            // change the translation
                            .map(translationBeforeMutation -> {
                                switch (translationBeforeMutation.languageName()) {
                                    case "english" -> {
                                        return LanguageTranslation.withNewTranslation(
                                                translationBeforeMutation,
                                                mutations.englishTranslation(),
                                                Instant.now().toEpochMilli(),
                                                translationBeforeMutation.userId()); // KGF : TODO : get user that is performing the updated
                                    }
                                    case "german" -> {
                                        return LanguageTranslation.withNewTranslation(
                                                translationBeforeMutation,
                                                mutations.germanTranslation(),
                                                Instant.now().toEpochMilli(),
                                                translationBeforeMutation.userId()); // KGF : TODO : get user that is performing the updated
                                    }
                                    default ->
                                            throw new RuntimeException("Unsupported language: " + translationBeforeMutation.languageName());
                                }
                            })
                            // collect the new translations
                            .collect(Collectors.toCollection(ArrayList::new));

                    System.out.println("Translations to be updated: " + mutating);

                    // execute updates
                    if (!mutating.isEmpty()) {
                        DBUtils.executeMultipleUpdatesTransactionally(DBQueries.generateUpdatedTranslationStatements(mutating));
                    } else {
                        System.out.println(">> Not updating, no mutations occured in translation.");
                    }

                    // return back the updated assets
                    var generatedHtml = generateAssetTranslationsHtml(mutations.assetId(), mf);

                    ctx.html(generatedHtml);
                })
                .start(7070);
    }

    private static String generateAssetTranslationsHtml(int assetId, MustacheFactory mf) {
        var translations = DBUtils.execute(DBQueries.getLanguageTranslationsForAsset(assetId))
                .stream()
                .map(languageTranslationFromObjectArray)
                .collect(Collectors.toCollection(ArrayList::new));
        var assets = DBUtils.execute(DBQueries.specificAssetSearch(assetId))
                .stream()
                .map(languageTranslationWithMetaFromResults)
                .map(matchFromLanguageTranslationWithMeta)
                .collect(Collectors.toCollection(ArrayList::new));
        if (assets.size() != 1)
            throw new RuntimeException("Found " + assets.size() + " assets with assetId " + assetId);
        var asset = assets.getFirst();

        var generatedHtml = generateAssetTranslationsTemplate(mf, translations, assetId, asset.title());
        return generatedHtml;
    }

    private static String generateAssetTranslationsTemplate(MustacheFactory mf, ArrayList<LanguageTranslation> translations, Integer assetId, String assetName) {
        Writer writer = new StringWriter();
        var assetTranslationsTemplate = mf.compile("localization.mustache");
        var scope = Map.of("assetId", assetId,
                "translations", translations,
                "assetName", assetName);
        var executedTemplate = assetTranslationsTemplate.execute(writer, scope);
        return executedTemplate.toString();
    }

    private static String getInspectAssetHtml(int assetId, MustacheFactory mf) {
        var assets = DBUtils.execute(DBQueries.specificAssetSearch(assetId))
                .stream()
                .map(languageTranslationWithMetaFromResults)
                .map(matchFromLanguageTranslationWithMeta)
                .collect(Collectors.toCollection(ArrayList::new));

        if (assets.size() != 1) {
            throw new RuntimeException("Got multiple assets back for specific asset search. Id: " + assetId + ". Results: " + assets);
        }

        var asset = assets.getFirst();
        System.out.println(">> got asset : " + asset);

        Writer writer = new StringWriter();
        var inspectAssetCompiledTemplate = mf.compile("inspect-asset.mustache");
        var generatedHtml = inspectAssetCompiledTemplate.execute(
                writer,
                Map.of("title", asset.title(),
                        "assetId", asset.assetId(),
                        "lastUpdatedTimeFormatted", asset.lastUpdatedTimeFormatted(),
                        "lastUpdatedBy", asset.lastUpdatedBy(),
                        "description", asset.description(),
                        "href", asset.href()));
        return generatedHtml.toString();
    }

    private static String generateSearchTemplate(MustacheFactory mf) {
        Writer writer1 = new StringWriter();
        var searchCompiledTemplate = mf.compile("search.mustache");
        return searchCompiledTemplate.execute(writer1, Map.of()).toString();
    }

    private static String generateQueryResultsTemplate(ArrayList<Match> matches, String searchLike, ArrayList<Match> nonMatches, MustacheFactory mf) {
        HashMap<String, Object> scopes = new HashMap<>();
        scopes.put("title", "Menu/HUD");
        scopes.put("records-found", matches.size());
        scopes.put("search", searchLike);
        scopes.put("filter", "");
        scopes.put("matches", matches);
        scopes.put("non-matches", nonMatches);

        try (Writer writer2 = new StringWriter()) {
            var queryResultsCompiledTemplate = mf.compile("query-results.mustache");
            return queryResultsCompiledTemplate.execute(writer2, scopes).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}