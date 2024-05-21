package org.example.db;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record LanguageTranslation(
        Integer assetId,
        String languageName,
        String translation,
        Long updated,
        String assetTitle,
        String updatedBy,
        Integer projectId,
        Integer languageId,
        Integer userId
) {
    public String lastUpdatedTimeFormatted() {
        var instant = Instant.ofEpochMilli(this.updated);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    public static LanguageTranslation withNewTranslation(LanguageTranslation original,
                                                         String newTranslation,
                                                         Long updated,
                                                         Integer userId) {
        return new LanguageTranslation(
                original.assetId,
                original.languageName,
                newTranslation,
                updated,
                original.assetTitle,
                original.updatedBy,
                original.projectId,
                original.languageId,
                userId
        );
    }
}
