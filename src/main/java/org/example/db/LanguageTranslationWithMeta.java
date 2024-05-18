package org.example.db;

public record LanguageTranslationWithMeta(
        Integer asset_id,
        String asset_name,
        String asset_url,
        String asset_description,
        String language_name,
        String project_name,
        String translation,
        String user_name,
        Long updated
) {
}
