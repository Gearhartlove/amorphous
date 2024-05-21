package org.example;

public record MutateTranslationRequest(
        Integer assetId,
        String englishTranslation,
        String germanTranslation
) {
}
