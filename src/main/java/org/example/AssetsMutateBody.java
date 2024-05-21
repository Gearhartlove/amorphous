package org.example;


public record AssetsMutateBody(
        String title,
        String url,
        String description,
        Integer assetId
) {
}
