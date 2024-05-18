package org.example;

import io.javalin.Javalin;
import org.example.db.DBUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        initTables();

        System.out.println(">> Serving Server");

        var app = Javalin.create(/*config*/)
                .get("/", ctx -> {
                    System.out.println(">> Serving Index");

                    try (var stream = Files.lines(Paths.get(Objects.requireNonNull(Main.class.getResource("../../menu.html")).toURI()))) {
                        String foo = stream.collect(Collectors.joining("\n"));
                        ctx.html(foo);
                    } catch (IOException | URISyntaxException e) {
                        e.printStackTrace();
                    }
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