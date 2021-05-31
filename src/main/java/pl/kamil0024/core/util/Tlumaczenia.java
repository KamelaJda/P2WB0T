/*
 *
 *    Copyright 2020 P2WB0T
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package pl.kamil0024.core.util;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kamil0024.core.Main;
import pl.kamil0024.core.logger.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

public class Tlumaczenia {

    private static final Logger logger = LoggerFactory.getLogger(Tlumaczenia.class);

    @Getter
    private static Properties languages = getProp();

    @Getter
    @Setter
    private static String lang;

    public Tlumaczenia() {
    }

    public static Properties getProp() {
        Properties p = new Properties();

        try {
            InputStream input = Main.class.getClassLoader().getResourceAsStream("pl.properties");
            p.load(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return p;
    }

    public static boolean load() {
        languages = getProp();
        logger.debug(get("translation.load"));
        return true;
    }

    public static String get(String key) {
        if (languages == null) throw new NullPointerException("getProp() == null");
        String a = repla(languages.getProperty(key, key));
        if (a.equals(key)) {
            Log.newError("Key %s nie jest przetłumaczony", Tlumaczenia.class, key);
        }
        return a;
    }

    public static String get(String key, Object... toReplace) {
        if (languages == null) throw new NullPointerException("getProp() == null");
        String property = get(key);
        ArrayList<String> parsedArray = new ArrayList<>();
        for (Object k : toReplace) {
            parsedArray.add(k.toString());
        }
        return repla(String.format(property, parsedArray.toArray()));
    }

    public static String get(String key, String... toReplace) {
        if (languages == null) throw new NullPointerException("getProp() == null");
        return repla(String.format(get(key), (Object[]) toReplace));
    }

    public static String repla(String key) {
        return key.replaceAll("Ä\u0085", "ą").
                replaceAll("Ä\u0087", "ć").
                replaceAll("Ä\u0099", "ę").
                replaceAll("Å\u0082", "ł").
                replaceAll("Å\u0084", "ń").
                replaceAll("Ã³", "ó").
                replaceAll("Å\u009B", "ś").
                replaceAll("Åº", "ź").
                replaceAll("Å¼", "ż").
                replaceAll("Å\u0081", "Ł");
    }


}
