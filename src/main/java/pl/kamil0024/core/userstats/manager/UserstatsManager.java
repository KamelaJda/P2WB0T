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
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.core.database.UserstatsDao;
import pl.kamil0024.core.database.config.UserstatsConfig;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.redis.Cache;
import pl.kamil0024.core.redis.RedisManager;
import pl.kamil0024.core.userstats.config.VoiceStateConfig;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UserstatsManager extends ListenerAdapter {

    public final RedisManager redisManager;
    public final UserstatsDao userstatsDao;

    public final ShardManager api;

    private final Cache<UserstatsConfig.Config> config;
    private final Cache<VoiceStateConfig> voiceStateConfig;

    public UserstatsManager(RedisManager redisManager, UserstatsDao userstatsDao, ShardManager api) {
        this.redisManager = redisManager;
        this.userstatsDao = userstatsDao;
        this.api = api;

        this.config = redisManager.new CacheRetriever<UserstatsConfig.Config>() {
        }.getCache(-1);
        this.voiceStateConfig = redisManager.new CacheRetriever<VoiceStateConfig>() {
        }.getCache(-1);

        ScheduledExecutorService executorSche = Executors.newSingleThreadScheduledExecutor();
        executorSche.scheduleWithFixedDelay(this::load, 30, 30, TimeUnit.MINUTES);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        if (!event.isFromGuild() || event.getAuthor().isBot()) return;

        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(event.getMessage().getTimeCreated().toInstant().toEpochMilli()));
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            String primKey = cal.getTime().getTime() + "-" + event.getMessage().getAuthor().getId();
            UserstatsConfig.Config conf = config.getOrElse(primKey, new UserstatsConfig.Config(0L, new HashMap<>()));

            conf.setMessageCount(conf.getMessageCount() + 1);

            long channelC = conf.getChannels().getOrDefault(event.getChannel().getId(), 0L);
            conf.getChannels().put(event.getChannel().getId(), channelC + 1L);

            config.put(primKey, conf);
        } catch (Exception e) {
            Log.newError(e, getClass());
        }

    }

    public void load() {

        new Thread(() -> {
            try {
                Set<Map.Entry<String, UserstatsConfig.Config>> saveConf = config.asMap().entrySet();
                Map<String, VoiceStateConfig> voiceConf = voiceStateConfig.asMap();
                try {
                    config.invalidateAll();
                } catch (JedisDataException ignored) {
                }
                try {
                    voiceStateConfig.invalidateAll();
                } catch (JedisDataException ignored) {
                }

                Date date = new Date();
                for (Map.Entry<String, VoiceStateConfig> entry : voiceConf.entrySet()) {
                    entry.getValue().setLastDate(date.getTime());
                    entry.getValue().setFullTimestamp(entry.getValue().getFullTimestamp() + (date.getTime() - entry.getValue().getLastDate()));
                }

                Map<Long, UserstatsConfig> map = new HashMap<>();

                for (Map.Entry<String, UserstatsConfig.Config> entry : saveConf) {
                    try {
                        String[] split = entry.getKey().split("::Config:")[1].split("-");
                        String sdate = split[0];
                        String member = split[1];
                        long ldate = Long.parseLong(sdate);

                        VoiceStateConfig vsc = voiceConf.get(sdate + "-" + member);

                        UserstatsConfig conf = map.getOrDefault(ldate, new UserstatsConfig(ldate));
                        conf.getMembers().put(member, entry.getValue());
                        conf.getMemberslist().remove(member);
                        conf.getMemberslist().add(member);
                        map.put(ldate, conf);

                    } catch (Exception e) {
                        Log.newError(e, getClass());
                    }
                }

                for (UserstatsConfig v : map.values()) {
                    UserstatsConfig dConf = userstatsDao.get(v.getDate());
                    if (dConf == null) {
                        userstatsDao.save(v);
                    } else {

                        for (Map.Entry<String, UserstatsConfig.Config> entry : v.getMembers().entrySet()) {
                            UserstatsConfig.Config c = dConf.getMembers().getOrDefault(entry.getKey(), new UserstatsConfig.Config(0L, new HashMap<>()));
                            c.setMessageCount(c.getMessageCount() + entry.getValue().getMessageCount());

                            for (Map.Entry<String, Long> channelEntry : entry.getValue().getChannels().entrySet()) {
                                long channelValue = c.getChannels().getOrDefault(channelEntry.getKey(), 0L);
                                c.getChannels().put(channelEntry.getKey(), channelValue + channelEntry.getValue());
                            }

                            dConf.getMembers().put(entry.getKey(), c);
                        }
                        userstatsDao.save(dConf);

                    }
                }
            } catch (Exception e) {
                Log.newError(e, getClass());
            }
        }).start();

    }

}
