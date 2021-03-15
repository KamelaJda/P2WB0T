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

package pl.kamil0024.core.util.kary;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kamil0024.core.Main;
import pl.kamil0024.core.logger.Log;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class KaryJSON {

    private static Logger logger = LoggerFactory.getLogger(KaryJSON.class);

    private ArrayList<Kara> kary;

    private InputStream is;
    private JSONTokener tokener;
    private JSONObject object = null;

    @SneakyThrows
    public KaryJSON() {
        this.kary = new ArrayList<>();

        try {
            this.is = Main.class.getClassLoader().getResourceAsStream("kary.json");
            if (is == null) throw new NullPointerException("kary.json jest nullem");
            this.tokener = new JSONTokener(is);
            this.object = new JSONObject(tokener).getJSONObject("list");
            loadKary();
        } catch (Exception e) {
            Log.newError(e, getClass());
        }
    }

    @SneakyThrows
    private void loadKary() {
        if (object == null) throw new UnsupportedOperationException("object przy ladowaniu kary jest nullem");
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

        Map<String, Kara> kary = gson.fromJson(object.toString(), new TypeToken<Map<String, Kara>>() {}.getType());
        Log.debug(gson.toJson(kary));

        for (Map.Entry<String, Kara> entry : kary.entrySet()) {
            Kara kara = entry.getValue();
            if (kara.getPowod() == null || kara.getTiery() == null || kara.getId() == null) {
                Log.newError("Kara o ID " + kara.getId() + " została źle wpisana!", getClass());
                continue;
            }
            kara.setPowod(repla(kara.getPowod()));
            logger.debug("------------------------");
            logger.debug("ID: " + kara.getId());
            logger.debug("Powod: " + kara.getPowod() + "\n");
            for (Tiery entryT : kara.getTiery()) {
                logger.debug("  Duration:" + entryT.getDuration());
                logger.debug("  Type:" + entryT.getType());
                logger.debug("  MaxWarns:" + entryT.getMaxWarns());
            }
            logger.debug("------------------------");
            getKary().add(kara);
        }

    }

    @Nullable
    public Kara getByName(String name) {
        return getKary().stream().filter(k -> k.getPowod().equalsIgnoreCase(name)).findAny().orElse(null);
    }

    private static String repla(String s) {
        return s.replaceAll("Ä…", "ą").
                replaceAll("Ä‡", "ć").
                replaceAll("Ä™", "ę").
                replaceAll("Ĺ‚", "ł").
                replaceAll("Ĺ„", "ń").
                replaceAll("Ăł", "ó").
                replaceAll("Ĺ›", "ś").
                replaceAll("Ĺş", "ź").
                replaceAll("ĹĽ", "ż");
    }

    @Data
    @AllArgsConstructor
    public static class Kara {
        public Kara() {}

        private Integer id;
        private String powod;
        private List<Tiery> tiery;

    }

    @Data
    @AllArgsConstructor
    public static class Tiery {
        private int maxWarns;
        private String duration;
        private KaryEnum type;
    }

}
