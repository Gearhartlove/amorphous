package org.example.query;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record Match(
        Integer assetId,
        String href,
        String title,
        String description,
        Long lastUpdatedTime, // epoch time
        String lastUpdatedBy
) {
    public String lastUpdatedTimeFormatted() {
        var instant = Instant.ofEpochMilli(this.lastUpdatedTime);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        String humanReadableTime = formatter.format(instant);
        return humanReadableTime;
    }
}
