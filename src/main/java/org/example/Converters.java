package org.example;

import org.example.db.LanguageTranslation;
import org.example.db.LanguageTranslationWithMeta;
import org.example.query.Match;

import java.util.function.Function;

public class Converters {
    public static final Function<Object[], LanguageTranslationWithMeta> languageTranslationWithMetaFromResults =
            (row -> new LanguageTranslationWithMeta(
                    (Integer) row[0],
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    (String) row[5],
                    (String) row[6],
                    (String) row[7],
                    (Long) row[8]
            ));

    public static final Function<LanguageTranslationWithMeta, Match> matchFromLanguageTranslationWithMeta =
            (lt -> new Match (
                    lt.asset_id(),
                    lt.asset_url(),
                    lt.asset_name(),
                    lt.asset_description(),
                    lt.updated(),
                    lt.user_name()
            ));

    public static final Function<Object[], LanguageTranslation> languageTranslationFromObjectArray =
            (objects -> new LanguageTranslation(
                    (Integer) objects[0], // asset id
                    (String) objects[1], // language name
                    (String) objects[2], // translation
                    (Long) objects[3], // updated
                    (String) objects[4], // asset name
                    (String) objects[5], // updated by
                    (Integer) objects[6], // project id
                    (Integer) objects[7], // language id
                    (Integer) objects[8] // user id
            ));
}
