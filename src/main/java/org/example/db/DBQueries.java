package org.example.db;

public class DBQueries {
    public static final String LANGUAGE_TRANSLATIONS_WITH_META = """
            SELECT asset_name, asset_url, asset_description, language_name, project_name, translation, user_name, max(updated)
            FROM language_translation
            JOIN main.asset a on language_translation.asset_id = a.asset_id
            JOIN main.project p on language_translation.project_id = p.project_id
            JOIN main.user u on language_translation.who_updated = u.user_id
            JOIN main.language_lookup ll on language_translation.language_id = ll.language_id
            GROUP BY language_translation.asset_id, language_translation.project_id;
            """;

    public static String menuHUDSearch(String searchLike) {
        return """
            SELECT asset_name, asset_url, asset_description, language_name, project_name, translation, user_name, max(updated)
            FROM language_translation
                     JOIN main.asset a on language_translation.asset_id = a.asset_id
                     JOIN main.project p on language_translation.project_id = p.project_id
                     JOIN main.user u on language_translation.who_updated = u.user_id
                     JOIN main.language_lookup ll on language_translation.language_id = ll.language_id
            WHERE asset_name like('%{{searchLike}}%') OR asset_description like('%{{searchLike}}%')
            GROUP BY language_translation.asset_id, language_translation.project_id;
            """.replace("{{searchLike}}", searchLike);
    }
}
