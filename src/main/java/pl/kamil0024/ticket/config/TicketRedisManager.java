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

package pl.kamil0024.ticket.config;

import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.redis.Cache;
import pl.kamil0024.core.redis.RedisManager;

public class TicketRedisManager {

    private final Cache<ChannelTicketConfig> channelCache;

    public TicketRedisManager(RedisManager redisManager) {
        this.channelCache = redisManager.new CacheRetriever<ChannelTicketConfig>() {}.getCache(3600);
    }

    @Nullable
    public ChannelTicketConfig getChannel(String id) {
        return channelCache.getIfPresent(id);
    }

    public void putChannelConfig(ChannelTicketConfig ctc) {
        if (ctc.getChannelId() == null) {
            Log.newError("<ChannelTicketConfig>.getChannelId() == null", TicketRedisManager.class);
            return;
        }
        channelCache.put(ctc.getChannelId(), ctc);
    }

    public void removeChannel(String id) {
        channelCache.invalidate(id);
    }

}



