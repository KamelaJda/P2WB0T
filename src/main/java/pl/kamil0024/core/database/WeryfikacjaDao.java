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
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.database.config.Dao;
import pl.kamil0024.core.database.config.WeryfikacjaConfig;

import java.util.List;

public class WeryfikacjaDao implements Dao<WeryfikacjaConfig> {

    private final PgMapper<WeryfikacjaConfig> mapper;

    public WeryfikacjaDao(DatabaseManager databaseManager) {
        if (databaseManager == null) throw new IllegalStateException("databaseManager == null");
        mapper = databaseManager.getPgStore().mapSync(WeryfikacjaConfig.class);
    }

    @Nullable
    @Override
    public WeryfikacjaConfig get(String mcnick) {
        return mapper.load(mcnick).orElse(null);
    }

    @Nullable
    public WeryfikacjaConfig getByDiscordId(String userId) {
        List<WeryfikacjaConfig> lista = mapper.loadRaw("SELECT * FROM %s WHERE data::jsonb @> '{\"discordId\": \"" + userId + "\"}'");
        return lista.isEmpty() ? null : lista.get(0);
    }

    @Override
    public void save(WeryfikacjaConfig toCos) {
        mapper.save(toCos);
    }

    @Override
    public List<WeryfikacjaConfig> getAll() {
        return mapper.loadAll();
    }

    public void bypass(String user) {
        List<WeryfikacjaConfig> lista = mapper.loadRaw("SELECT * FROM %s WHERE data::jsonb @> '{\"discordId\": \"" + user + "\"}'");
        for (WeryfikacjaConfig entry : lista) {
            entry.setDisabled(true);
            save(entry);
        }

    }

}
