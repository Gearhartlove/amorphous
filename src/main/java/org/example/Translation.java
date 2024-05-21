package org.example;

public record Translation(
        String translation,
        Long updated,
        String updatedBy
) {
}
