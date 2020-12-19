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

package pl.kamil0024.logs.logger;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import pl.kamil0024.core.redis.Cache;
import pl.kamil0024.core.redis.RedisManager;

import javax.annotation.Nonnull;

public class MessageManager extends ListenerAdapter {

    @Getter private final Cache<FakeMessage> map;

    public MessageManager(RedisManager redisManager) {
        this.map = redisManager.new CacheRetriever<FakeMessage>() {}.getCache(2629743);
    }

    public void add(Message message) {
        map.put(message.getId(), FakeMessage.convert(message));
    }

    public void edit(Message message) {
        map.invalidate(message.getId());
        add(message);
    }

    public boolean exists(String id) {
        return map.getIfPresent(id) != null;
    }

    public FakeMessage get(String id) {
        if (!exists(id)) return null;
        return map.getIfPresent(id);
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getAuthor().isFake() || event.getMessage().isWebhookMessage() ||
                event.getMessage().getContentRaw().isEmpty()) return;
        add(event.getMessage());
    }

}

