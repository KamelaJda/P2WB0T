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

package pl.kamil0024.core.userstats.manager;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import pl.kamil0024.core.database.UserstatsDao;
import pl.kamil0024.core.database.config.UserstatsConfig;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.redis.Cache;
import pl.kamil0024.core.redis.RedisManager;
import pl.kamil0024.core.util.GsonUtil;

import javax.annotation.Nonnull;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UserstatsManager extends ListenerAdapter {

    public final RedisManager redisManager;
    public final UserstatsDao userstatsDao;

    private final Cache<UserstatsConfig.Config> config;

    public UserstatsManager(RedisManager redisManager, UserstatsDao userstatsDao) {
        this.redisManager = redisManager;
        this.userstatsDao = userstatsDao;
        this.config = redisManager.new CacheRetriever<UserstatsConfig.Config>() {}.getCache(-1);

        ScheduledExecutorService executorSche = Executors.newScheduledThreadPool(2);
        executorSche.scheduleAtFixedRate(this::load, 30, 30, TimeUnit.MINUTES);

    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot()) return;

        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(event.getMessage().getTimeCreated().toInstant().toEpochMilli()));
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            String primKey = cal.getTime().getTime() + "-" + event.getMessage().getAuthor().getId();
            UserstatsConfig.Config conf = config.getOrElse(primKey, new UserstatsConfig.Config(0, new HashMap<>()));

            conf.setMessageCount(conf.getMessageCount() + 1);

            long channelC = conf.getChannels().getOrDefault(event.getChannel().getId(), 0L);
            conf.getChannels().put(event.getChannel().getId(), channelC + 1L);

            config.put(primKey, conf);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void load() {

        Map<Long, UserstatsConfig> map = new HashMap<>();

        for (Map.Entry<String, UserstatsConfig.Config> entry : config.asMap().entrySet()) {
            try {
                Log.debug("entry.getKey(): " + entry.getKey());
                String[] split = entry.getKey().split("-");
                String sdate = split[0];
                String member = split[1];
                long ldate = Long.parseLong(sdate);

                UserstatsConfig conf = map.getOrDefault(ldate, new UserstatsConfig(ldate));
                conf.getMembers().put(member, entry.getValue());
            } catch (Exception e) {
                e.printStackTrace();
//                Log.newError(e, getClass());
            }
        }

        Log.debug(GsonUtil.toJSON(map));
        for (UserstatsConfig v : map.values()) {
            userstatsDao.save(v);
        }

    }

}
