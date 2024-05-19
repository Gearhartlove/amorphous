package org.example.db;

import java.util.List;
import java.util.stream.Collectors;

public class DBQueries {

    public static void initTables() {
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

    public static final String LANGUAGE_TRANSLATIONS_WITH_META = """
            SELECT language_translation.asset_id, asset_name, asset_url, asset_description, language_name, project_name, translation, user_name, max(updated)
            FROM language_translation
            JOIN main.asset a on language_translation.asset_id = a.asset_id
            JOIN main.project p on language_translation.project_id = p.project_id
            JOIN main.user u on language_translation.who_updated = u.user_id
            JOIN main.language_lookup ll on language_translation.language_id = ll.language_id
            GROUP BY language_translation.asset_id, language_translation.project_id;
            """;

    public static String menuHUDSearch(String searchLike) {
        return """
                SELECT language_translation.asset_id, asset_name, asset_url, asset_description, language_name, project_name, translation, user_name, max(updated)
                FROM language_translation
                         JOIN main.asset a on language_translation.asset_id = a.asset_id
                         JOIN main.project p on language_translation.project_id = p.project_id
                         JOIN main.user u on language_translation.who_updated = u.user_id
                         JOIN main.language_lookup ll on language_translation.language_id = ll.language_id
                WHERE asset_name like('%{{searchLike}}%') OR asset_description like('%{{searchLike}}%')
                GROUP BY language_translation.asset_id, language_translation.project_id;
                """.replace("{{searchLike}}", searchLike);
    }

    public static String menuHUDReverseSearch(List<Integer> assetIds) {
        var query = """
                -- reverse query
                SELECT language_translation.asset_id,
                       asset_name,
                       asset_url,
                       asset_description,
                       language_name,
                       project_name,
                       translation,
                       user_name,
                       max(updated)
                FROM language_translation
                         JOIN main.asset a on language_translation.asset_id = a.asset_id
                         JOIN main.project p on language_translation.project_id = p.project_id
                         JOIN main.user u on language_translation.who_updated = u.user_id
                         JOIN main.language_lookup ll on language_translation.language_id = ll.language_id
                WHERE language_translation.asset_id NOT IN ({{assetIds}})
                GROUP BY language_translation.asset_id, language_translation.project_id;
                """.replace("{{assetIds}}", assetIds.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        System.out.println(">> executing reverse query: " + "\n" + query);
        return query;
    }
}
