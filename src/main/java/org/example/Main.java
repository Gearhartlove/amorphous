package org.example;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import io.javalin.Javalin;
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

public class Main {
    public static void main(String[] args) {
        MustacheFactory mf = new DefaultMustacheFactory();

        initTables();

        System.out.println(">> Serving Server");

        var app = Javalin.create(/*config*/)
                .get("/", ctx -> {
                    System.out.println(">> Serving Index");

                    try (var stream = Files.lines(Paths.get(Objects.requireNonNull(Main.class.getResource("../../index.html")).toURI()))) {
                        String foo = stream.collect(Collectors.joining("\n"));
                        ctx.html(foo);
                    } catch (IOException | URISyntaxException e) {
                        e.printStackTrace();
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

                    var nonMatches = new ArrayList<Match>();

                    // TODO : no code duplication
                    var queryResultsExecutedTemplate = generateQueryResultsTemplate(matches, searchLike, nonMatches, mf);

                    ctx.html(queryResultsExecutedTemplate.toString());
                })
                .get("/menu-hud", ctx -> {
                    System.out.println(">> Serving Menu/HUD");

                    var matches = DBUtils.execute(DBQueries.LANGUAGE_TRANSLATIONS_WITH_META)
                            .stream()
                            .map(languageTranslationWithMetaFromResults)
                            .map(matchFromLanguageTronslationWithMeta)
                            .collect(Collectors.toCollection(ArrayList::new));

                    var nonMatches = new ArrayList<Match>();

                    var searchExecutedTemplate = generateSearchTemplate(mf, matches);
                    var queryResultsExecutedTemplate = generateQueryResultsTemplate(matches, null, nonMatches, mf);

                    ctx.html(searchExecutedTemplate.toString() + queryResultsExecutedTemplate.toString());
                })
                .start(7070);
    }

    private static Writer generateSearchTemplate(MustacheFactory mf, ArrayList<Match> matches) {
        Writer writer1 = new StringWriter();
        var searchCompiledTemplate = mf.compile("search.mustache");
        return searchCompiledTemplate.execute(writer1, Map.of());
    }

    private static Writer generateQueryResultsTemplate(ArrayList<Match> matches, String searchLike, ArrayList<Match> nonMatches, MustacheFactory mf) {
        HashMap<String, Object> scopes = new HashMap<>();
        scopes.put("title", "Menu/HUD");
        scopes.put("records-found", matches.size());
        scopes.put("search", searchLike);
        scopes.put("filter", "");
        scopes.put("matches", matches);
        scopes.put("non-matches", nonMatches);

        try(Writer writer2 = new StringWriter()) {
            var queryResultsCompiledTemplate = mf.compile("query-results.mustache");
            return queryResultsCompiledTemplate.execute(writer2, scopes);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void initTables() {
        createLanguageTranslationTable();
        createLanguageLookupTable();
        createProjectTable();
        createUserTable();
        createAssetTable();
    }

    private static void createLanguageLookupTable() {
        System.out.println(">> Creating Language Lookup Table");

        DBUtils.execute("""
                CREATE TABLE IF NOT EXISTS language_lookup (
                    language_id PRIMARY KEY NOT NULL,
                    language_name VARCHAR(50) NOT NULL,
                    language_code VARCHAR(10) NOT NULL
                )
                """);

        System.out.println(">> Created Language Lookup Table");
    }

    public static void createLanguageTranslationTable() {
        System.out.println(">> Creating Language Translation Table");

        DBUtils.execute("""
                CREATE TABLE IF NOT EXISTS language_translation (
                    asset_id INT,
                    project_id INT,
                    language_id INT,
                    translation TEXT,
                    updated DATETIME,
                    who_updated INT,
                    PRIMARY KEY (asset_id, project_id, language_id)
                );""");

        System.out.println(">> Created Language Translation Table");
    }

    public static void createAssetTable() {
        System.out.println(">> Creating Asset Table");

        DBUtils.execute("""
                CREATE TABLE IF NOT EXISTS asset (
                    asset_id INTEGER PRIMARY KEY,
                    asset_name VARCHAR(50) NOT NULL,
                    asset_url  VARCHAR(50) NOT NULL
                );""");

        System.out.println(">> Created Asset Table");
    }

    public static void createProjectTable() {
        System.out.println(">> Creating Project Table");

        DBUtils.execute("""
                CREATE TABLE IF NOT EXISTS project (
                    project_id INTEGER PRIMARY KEY,
                    project_name VARCHAR(50) NOT NULL
                );""");

        System.out.println(">> Create Project Table");
    }

    public static void createUserTable() {
        System.out.println(">> Creating User Table");

        DBUtils.execute("""
                CREATE TABLE IF NOT EXISTS user (
                    user_id INTEGER PRIMARY KEY,
                    user_name VARCHAR(50) NOT NULL
                );""");

        System.out.println(">> Created User Table");
    }
}