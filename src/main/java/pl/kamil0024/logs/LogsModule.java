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

package pl.kamil0024.logs;

import net.dv8tion.jda.api.sharding.ShardManager;
import org.joda.time.DateTime;
import pl.kamil0024.core.database.DeletedMessagesDao;
import pl.kamil0024.core.database.config.DeletedMessagesConfig;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.redis.RedisManager;
import pl.kamil0024.logs.logger.Logger;
import pl.kamil0024.logs.logger.MessageManager;
import pl.kamil0024.stats.StatsModule;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LogsModule implements Modul {
    
    @Inject ShardManager api;

    private boolean start = false;

    private MessageManager messageManager;
    private Logger logger;
    private final StatsModule statsModule;
    private final RedisManager redisManager;
    private final DeletedMessagesDao deletedMessagesDao;

    public LogsModule(ShardManager api, StatsModule statsModule, RedisManager redisManager, DeletedMessagesDao deletedMessagesDao) {
        this.api = api;
        this.statsModule = statsModule;
        this.redisManager = redisManager;
        this.deletedMessagesDao = deletedMessagesDao;

        ScheduledExecutorService executorSche = Executors.newScheduledThreadPool(2);
        executorSche.scheduleAtFixedRate(() -> {
            for (DeletedMessagesConfig entry : deletedMessagesDao.getAll()) {
                if (new DateTime(entry.getDeletedDate()).plusDays(7).isBeforeNow()) {
                    deletedMessagesDao.delete(entry.getId());
                }
            }
        }, 1, 1, TimeUnit.HOURS);

    }
    
    @Override
    public boolean startUp() {
        messageManager = new MessageManager(redisManager);
        logger = new Logger(messageManager, api, statsModule, deletedMessagesDao);
        api.addEventListener(messageManager, logger);
        setStart(true);
        return true;
    }

    @Override
    public boolean shutDown() {
        api.removeEventListener(messageManager, logger);
        setStart(false);
        return true;
    }

    @Override
    public String getName() {
        return "logs";
    }

    @Override
    public boolean isStart() {
        return this.start;
    }

    @Override
    public void setStart(boolean bol) {
        this.start = bol;
    }

}
