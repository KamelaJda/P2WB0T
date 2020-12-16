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

package pl.kamil0024.embedgenerator.entity;

import net.dv8tion.jda.api.EmbedBuilder;
import pl.kamil0024.core.redis.Cache;
import pl.kamil0024.core.redis.RedisManager;

import javax.annotation.Nullable;

public class EmbedRedisManager {

    private final Cache<EmbedBuilder> cache;

    public EmbedRedisManager(RedisManager redisManager) {
        this.cache = redisManager.new CacheRetriever<EmbedBuilder>(){}.getCache(3600);
    }

    @Nullable
    public EmbedBuilder get(String code) {
        return cache.getIfPresent(code);
    }

    public void save(String code, EmbedBuilder embedBuilder) {
        cache.put(code, embedBuilder);
    }

}
