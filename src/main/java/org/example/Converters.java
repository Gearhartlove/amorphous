package org.example;

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

    public static final Function<LanguageTranslationWithMeta, Match> matchFromLanguageTronslationWithMeta =
            (lt -> new Match (
                    lt.asset_id(),
                    lt.asset_url(),
                    lt.asset_name(),
                    lt.asset_description(),
                    lt.updated(), // KGF : TODO figure out date time
                    lt.user_name()
            ));
}
