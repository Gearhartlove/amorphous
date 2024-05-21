package org.example;

public record TranslationMatch(
        String languageName,
        String translation,
        Long updated,
        String updatedBy
) {
}
