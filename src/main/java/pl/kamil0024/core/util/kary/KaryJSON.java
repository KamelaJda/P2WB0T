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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kamil0024.core.Main;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.GsonUtil;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Data
public class KaryJSON {

    private static final Logger logger = LoggerFactory.getLogger(KaryJSON.class);

    private ArrayList<Kara> kary;
    private JsonObject object = null;

    @SneakyThrows
    public KaryJSON() {
        this.kary = new ArrayList<>();

        try {
            InputStream is = Main.class.getClassLoader().getResourceAsStream("kary.json");
            if (is == null) throw new NullPointerException("kary.json jest nullem");
            this.object = GsonUtil.GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class).getAsJsonObject("list");

            loadKary();
        } catch (Exception e) {
            Log.newError(e, getClass());
        }
    }

    @SneakyThrows
    private void loadKary() {
        if (object == null) throw new UnsupportedOperationException("object przy ladowaniu kar jest nullem");

        for (int i = 1; i < object.size(); i++) {
            JsonObject jsonKara = object.getAsJsonObject(String.valueOf(i));
            if (jsonKara == null) continue;

            Log.debug("-----------------------------");
            Kara kara = new Kara(i, jsonKara.get("name").getAsString());
            Log.debug("Nowa kara");
            Log.debug(GsonUtil.GSON.toJson(kara));

            for (int ii = 1; ii < 30; ii++) {
                JsonElement je = jsonKara.get("tier_" + ii);
                Log.debug("Nowy tier");
                Log.debug(GsonUtil.GSON.toJson(je));
                if (je == null) continue;
                kara.getTiery().add(GsonUtil.GSON.fromJson(je, Tiery.class));
            }
            Log.debug("-----------------------------");

            getKary().add(kara);
        }

    }

    @Nullable
    public Kara getByName(String name) {
        return getKary().stream().filter(k -> k.getPowod().equalsIgnoreCase(name)).findAny().orElse(null);
    }

    @Data
    @AllArgsConstructor
    public static class Kara {
        public Kara() {}

        private Integer id;
        private String powod;
        private final List<Tiery> tiery = new ArrayList<>();

    }

    @Data
    @AllArgsConstructor
    public static class Tiery {
        private int maxWarns;
        @SerializedName("time")
        private String duration;
        private KaryEnum type;
    }

}
