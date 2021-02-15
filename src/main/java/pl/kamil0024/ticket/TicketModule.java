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

package pl.kamil0024.ticket;

import com.google.inject.Inject;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.core.database.TicketDao;
import pl.kamil0024.core.database.config.TicketConfig;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.redis.RedisManager;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.ticket.config.TicketRedisManager;
import pl.kamil0024.ticket.listener.VoiceChatListener;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TicketModule implements Modul {

    @Inject TicketDao ticketDao;
    @Inject ShardManager api;
    @Inject RedisManager redisManager;
    @Inject EventWaiter eventWaiter;

    private boolean start = false;

    private final TicketRedisManager ticketRedisManager;

    // Listeners
    private VoiceChatListener vcl;

    public TicketModule(ShardManager api, TicketDao ticketDao, RedisManager redisManager, EventWaiter eventWaiter) {
        this.api = api;
        this.ticketDao = ticketDao;
        this.redisManager = redisManager;
        this.ticketRedisManager = new TicketRedisManager(redisManager);
        this.eventWaiter = eventWaiter;
    }

    @Override
    public boolean startUp() {
        vcl = new VoiceChatListener(ticketDao, ticketRedisManager, eventWaiter, redisManager);
        api.addEventListener(vcl);
        return true;
    }

    @Override
    public boolean shutDown() {
        api.removeEventListener(vcl);
        return true;
    }

    @Override
    public String getName() {
        return "ticket";
    }

    @Override
    public boolean isStart() {
        return start;
    }

    @Override
    public void setStart(boolean bol) {
        this.start = bol;
    }
}
