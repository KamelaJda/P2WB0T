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

package pl.kamil0024.userstats.manager;

import net.dv8tion.jda.api.entities.Message;
import pl.kamil0024.core.database.UserstatsDao;
import pl.kamil0024.core.database.config.UserstatsConfig;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.redis.Cache;
import pl.kamil0024.core.redis.RedisManager;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StatsManager {

    private final Cache<UserstatsConfig> statsCache;
    private final UserstatsDao userstatsDao;

    public StatsManager(RedisManager redisManager, UserstatsDao userstatsDao) {
        this.statsCache = redisManager.new CacheRetriever<UserstatsConfig>(){}.getCache(-1);
        this.userstatsDao = userstatsDao;

        ScheduledExecutorService executorSche = Executors.newScheduledThreadPool(2);
        executorSche.scheduleAtFixedRate(() -> {
            try {
                save();
            } catch (Exception e) {
                Log.newError(e, getClass());
            }
        }, 15, 15, TimeUnit.MINUTES);
    }

    public void put(Message msg) {
        UserstatsConfig stat = statsCache.getifPresentOr(format(msg),
                new UserstatsConfig(String.valueOf(msg.getTimeCreated().toInstant().getEpochSecond()), msg.getAuthor().getId(), 0, msg.getChannel().getId()));
        stat.setMessages(stat.getMessages() + 1);
        statsCache.put(format(msg), stat);
    }

    public void save() {
        final Calendar cal = Calendar.getInstance();
        final Set<Map.Entry<String, UserstatsConfig>> tak = statsCache.asMap().entrySet();
        statsCache.invalidateAll();

        new Thread(() -> {
            for (Map.Entry<String, UserstatsConfig> entry : tak) {
                UserstatsConfig value = entry.getValue();

                cal.setTime(new Date(Long.parseLong(value.getDate())));
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                value.setDate(String.valueOf(cal.getTime().getTime()));

                userstatsDao.save(value);
            }
        }).start();

    }

    private String format(Message msg) {
        return msg.getAuthor().getId() + "-" + msg.getChannel().getId();
    }

}
