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

package pl.kamil0024.status;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.redis.Cache;
import pl.kamil0024.core.redis.RedisManager;
import pl.kamil0024.core.util.NetworkUtil;
import pl.kamil0024.status.statusy.WulgarneStatusy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StatusModule implements Modul {

    private final ShardManager api;
    public final Cache<String> cache;

    public StatusModule(ShardManager api, RedisManager redisManager) {
        this.api = api;
        this.cache = redisManager.new CacheRetriever<String>() {}.getCache(300);
    }

    private boolean start = false;
    private final WulgarneStatusy wulgarneStatusy = new WulgarneStatusy();

    @Override
    public boolean startUp() {
        ScheduledExecutorService executorSche = Executors.newSingleThreadScheduledExecutor();
        executorSche.scheduleAtFixedRate(() -> check(api), 0, 5, TimeUnit.MINUTES);

        setStart(true);
        return true;
    }

    public void check(ShardManager api) {
        try {
            Map<String, String> statusy = new HashMap<>();
            Guild g = api.getGuildById(Ustawienia.instance.bot.guildId);
            if (g != null) {

                for (Member member : g.getMembers()) {
                    List<String> atc = wulgarneStatusy.getAvtivity(member);
                    if (atc.isEmpty() || cache.getIfPresent(member.getId()) != null) continue;
                    String swear = wulgarneStatusy.containsSwear(atc);
                    if (swear != null) statusy.put(member.getId(), swear);
                    else {
                        swear = wulgarneStatusy.containsLink(atc);
                        if (swear != null) statusy.put(member.getId(), swear);
                    }
                }

                for (Map.Entry<String, String> entry : statusy.entrySet()) {
                    cache.put(entry.getKey(), entry.getValue());
                }

            }

         } catch (Exception e) {
            Log.newError(e, getClass());
        }
    }

    @Override
    public boolean shutDown() {
        setStart(false);
        return true;
    }

    @Override
    public String getName() {
        return "status";
    }

    @Override
    public boolean isStart() {
        return start;
    }

    @Override
    public void setStart(boolean bol) {
        start = bol;
    }

}
