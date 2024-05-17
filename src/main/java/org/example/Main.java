package org.example;

import org.example.db.DBUtils;

public class Main {
    public static void main(String[] args) {
        createLanguageTranslationTable();
        createLanguageLookupTable();
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
                CREATE TABLE IF NOT EXISTS languageTranslation (
                    asset_id INT,
                    project_id INT,
                    language_id INT,
                    translation TEXT,
                    updated DATETIME,
                    who_updated TEXT,
                    PRIMARY KEY (asset_id, project_id)
                );""");

        System.out.println(">> Created Language Translation Table");
    }
}