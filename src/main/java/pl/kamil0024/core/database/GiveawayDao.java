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
import pl.kamil0024.core.database.config.GiveawayConfig;

import java.util.List;

public class GiveawayDao implements Dao<GiveawayConfig> {

    @Getter
    private final PgMapper<GiveawayConfig> mapper;

    public GiveawayDao(DatabaseManager databaseManager) {
        if (databaseManager == null) throw new IllegalStateException("databaseManager == null");
        mapper = databaseManager.getPgStore().mapSync(GiveawayConfig.class);
    }


    @Override
    @Nullable
    public GiveawayConfig get(String id) {
        return mapper.load(id).orElse(null);
    }

    public GiveawayConfig get() {
        return new GiveawayConfig(String.valueOf(mapper.loadAll().size() + 1));
    }

}
