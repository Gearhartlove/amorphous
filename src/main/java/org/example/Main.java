package org.example;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinJackson;
import io.javalin.json.JsonMapper;
import org.example.db.DBQueries;
import org.example.db.DBUtils;
import org.example.query.Match;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.example.Converters.languageTranslationWithMetaFromResults;
import static org.example.Converters.matchFromLanguageTronslationWithMeta;
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
                            .map(matchFromLanguageTronslationWithMeta)
                            .collect(Collectors.toCollection(ArrayList::new));

                    var nonMatches = DBUtils.execute(
                                    DBQueries.menuHUDReverseSearch(
                                            matches.stream()
                                                    .map(Match::assetId)
                                                    .collect(Collectors.toCollection(ArrayList::new))))
                            .stream()
                            .map(languageTranslationWithMetaFromResults)
                            .map(matchFromLanguageTronslationWithMeta)
                            .collect(Collectors.toCollection(ArrayList::new));

                    var queryResultsExecutedTemplate = generateQueryResultsTemplate(matches, searchLike, nonMatches, mf);

                    ctx.html(queryResultsExecutedTemplate);
                })
                .get("/menu-hud", ctx -> {
                    System.out.println(">> Serving Menu/HUD");

                    var matches = DBUtils.execute(DBQueries.LANGUAGE_TRANSLATIONS_WITH_META)
                            .stream()
                            .map(languageTranslationWithMetaFromResults)
                            .map(matchFromLanguageTronslationWithMeta)
                            .collect(Collectors.toCollection(ArrayList::new));

                    var nonMatches = DBUtils.execute(
                                    DBQueries.menuHUDReverseSearch(
                                            matches.stream()
                                                    .map(Match::assetId)
                                                    .collect(Collectors.toCollection(ArrayList::new))))
                            .stream()
                            .map(languageTranslationWithMetaFromResults)
                            .map(matchFromLanguageTronslationWithMeta)
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
                .start(7070);
    }

    private static String getInspectAssetHtml(int assetId, MustacheFactory mf) {
        var assets = DBUtils.execute(DBQueries.specificAssetSearch(assetId))
                .stream()
                .map(languageTranslationWithMetaFromResults)
                .map(matchFromLanguageTronslationWithMeta)
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