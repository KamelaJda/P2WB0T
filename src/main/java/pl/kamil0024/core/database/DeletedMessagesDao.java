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
import pl.kamil0024.core.database.config.DeletedMessagesConfig;

import java.util.List;

public class DeletedMessagesDao implements Dao<DeletedMessagesConfig>  {

    public final PgMapper<DeletedMessagesConfig> mapper;

    public DeletedMessagesDao(DatabaseManager databaseManager) {
        if (databaseManager == null) throw new IllegalStateException("databaseManager == null");
        mapper = databaseManager.getPgStore().mapSync(DeletedMessagesConfig.class);
    }

    @Override
    @Nullable
    public DeletedMessagesConfig get(String id) {
        return mapper.load(id).orElse(null);
    }

    public List<DeletedMessagesConfig> getFromChannel(String channelId, int offset) {
        return mapper.loadRaw(String.format("SELECT * FROM deletedmessage WHERE data->>'channelId' = '%s' ORDER BY data->>'deletedDate' DESC LIMIT 10 OFFSET %d;", channelId, offset));
    }

    public List<DeletedMessagesConfig> getFromUser(String userId, int offset) {
        return mapper.loadRaw(String.format("SELECT * FROM deletedmessage WHERE data->>'userId' = '%s' ORDER BY data->>'deletedDate' DESC LIMIT 10 OFFSET %d;", userId, offset));
    }

    public void delete(String id) {
        mapper.getStore().sql(String.format("DELETE FROM deletedmessage WHERE id='%s'"));
    }

    @Override
    public void save(DeletedMessagesConfig toCos) {
        mapper.save(toCos);
    }

    @Override
    public List<DeletedMessagesConfig> getAll() {
        return mapper.loadAll();
    }

    public List<DeletedMessagesConfig> getAll(int offset) {
        return mapper.getAllLogs(offset);
    }

}
