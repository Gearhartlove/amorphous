package org.example;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import io.javalin.Javalin;
import org.example.db.DBQueries;
import org.example.db.DBUtils;
import org.example.db.LanguageTranslationWithMeta;
import org.example.query.Match;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

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
                .get("/menu-hud", ctx -> {
                    System.out.println(">> Serving Menu/HUD");

                    var results = DBUtils.execute(DBQueries.LANGUAGE_TRANSLATIONS_WITH_META);
                    ArrayList<Match> matches = results.stream()
                            .map(row -> new LanguageTranslationWithMeta(
                                    (String) row[0],
                                    (String) row[1],
                                    (String) row[2],
                                    (String) row[3],
                                    (String) row[4],
                                    (String) row[5],
                                    (String) row[6],
                                    (Long) row[7]
                            ))
                            .map(lt -> new Match(
                                    lt.asset_url(),
                                    lt.asset_name(),
                                    lt.asset_description(),
                                    lt.updated(), // KGF : TODO figure out date time
                                    lt.user_name()
                            ))
                            .collect(Collectors.toCollection(ArrayList::new));

                    var recordsFound = matches.size(); // TODO turn into query

                    ArrayList<Match> nonMatches = new ArrayList<>(); // initialize to 0 non-matches because we select *

                    HashMap<String, Object> scopes = new HashMap<>();
                    scopes.put("title", "Menu/HUD");
                    scopes.put("records-found", recordsFound);
                    scopes.put("search", "");
                    scopes.put("filter", "");
                    scopes.put("matches", matches);
                    scopes.put("non-matches", nonMatches);

                    Writer writer = new StringWriter();
                    var compiledTemplate = mf.compile("query.mustache");
                    var executedTemplate = compiledTemplate.execute(writer, scopes);

                    ctx.html(executedTemplate.toString());
                })
                .start(7070);
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