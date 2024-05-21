package org.example;

public record AssetTranslations(
        Integer assetId,
        Translation englishTranslation,
        Translation germanTranslation
) {
}
