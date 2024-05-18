package org.example.query;

public record Match(
        String href,
        String title,
        String description,
        Double lastUpdatedTime, // epoch time
        String lastUpdatedBy
) {
}
