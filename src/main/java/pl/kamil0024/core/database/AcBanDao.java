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
import pl.kamil0024.core.database.config.AcBanConfig;
import pl.kamil0024.core.database.config.Dao;

import java.util.List;

public class AcBanDao implements Dao<AcBanConfig> {

    @Getter
    private final PgMapper<AcBanConfig> mapper;

    public AcBanDao(DatabaseManager databaseManager) {
        if (databaseManager == null) throw new IllegalStateException("databaseManager == null");
        mapper = databaseManager.getPgStore().mapSync(AcBanConfig.class);
    }

    @Override
    public AcBanConfig get(String id) {
        return mapper.load(id).orElse(null);
    }

    public List<AcBanConfig> getAll(int offset, boolean seeReaded) {
        return mapper.getAllAcBan(offset, seeReaded);
    }

    public boolean existNick(String nick) {
        return !mapper.loadRaw("SELECT * FROM acban WHERE data::jsonb @> '{\"nick\": \"" + nick + "\"}'").isEmpty();
    }

}
