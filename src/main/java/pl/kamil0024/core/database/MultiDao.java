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
import pl.kamil0024.core.database.config.Dao;
import pl.kamil0024.core.database.config.MultiConfig;
import pl.kamil0024.core.util.Nick;

import java.util.List;

public class MultiDao implements Dao<MultiConfig> {

    private final PgMapper<MultiConfig> mapper;

    public MultiDao(DatabaseManager databaseManager) {
        if (databaseManager == null) throw new IllegalStateException("databaseManager == null");
        mapper = databaseManager.getPgStore().mapSync(MultiConfig.class);
    }

    @Override
    public MultiConfig get(String id) {
        return mapper.load(id).orElseGet(() -> new MultiConfig(id));
    }

    public List<MultiConfig> getByNick(String nick) {
        List<MultiConfig> conf = mapper.loadRaw(String.format("SELECT * FROM multi WHERE data::jsonb @> '{\"nicki\": [{\"nick\": \"%s\"}]}';", nick));
        for (MultiConfig config : conf) {
            config.getNicki().removeIf(nick1 -> !nick1.getNick().equals(nick));
        }
        return conf;

    }

    @Override
    public void save(MultiConfig toCos) {
        mapper.save(toCos);
    }

    @Override
    public List<MultiConfig> getAll() {
        return mapper.loadAll();
    }

}
