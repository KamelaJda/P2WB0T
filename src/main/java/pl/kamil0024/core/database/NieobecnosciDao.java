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

package pl.kamil0024.core.database;

import gg.amy.pgorm.PgMapper;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.database.config.Dao;
import pl.kamil0024.core.database.config.NieobecnosciConfig;
import pl.kamil0024.nieobecnosci.config.Nieobecnosc;

import java.util.ArrayList;
import java.util.List;

public class NieobecnosciDao implements Dao<NieobecnosciConfig> {

    @Getter
    private final PgMapper<NieobecnosciConfig> mapper;

    public NieobecnosciDao(DatabaseManager databaseManager) {
        if (databaseManager == null) throw new IllegalStateException("databaseManager == null");
        mapper = databaseManager.getPgStore().mapSync(NieobecnosciConfig.class);
    }

    @Override
    public NieobecnosciConfig get(String id) {
        return mapper.load(id).orElseGet(() -> newObject(id));
    }

    private NieobecnosciConfig newObject(String id) {
        return new NieobecnosciConfig(id);
    }

    public int getNextId(String id) {
        return get(id).getNieobecnosc().size() + 1;
    }


    public ArrayList<Nieobecnosc> getAllAktywne() {
        ArrayList<Nieobecnosc> xd = new ArrayList<>();
        for (NieobecnosciConfig conf : getAll()) {
            for (Nieobecnosc nieobecnosc : conf.getNieobecnosc()) {
                if (nieobecnosc.isAktywna()) xd.add(nieobecnosc);
            }
        }
        return xd;
    }

    public boolean hasNieobecnosc(String id) {
        for (Nieobecnosc nieobecnosc : get(id).getNieobecnosc()) {
            if (nieobecnosc.isAktywna()) return true;
        }
        return false;
    }

    @Nullable
    public Nieobecnosc lastNieobecnosc(String id) {
        Nieobecnosc xd = null;
        for (Nieobecnosc nieobecnosc : get(id).getNieobecnosc()) {
            xd = nieobecnosc;
        }
        return xd;
    }

}
