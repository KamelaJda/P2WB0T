package pl.kamil0024.privatechannel;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.core.database.TXTTicketDao;
import pl.kamil0024.core.database.TicketDao;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.redis.RedisManager;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.privatechannel.listeners.PVChannelListener;
import pl.kamil0024.ticket.config.TicketRedisManager;

import java.util.ArrayList;
import java.util.List;

public class PVChannelModule implements Modul {

    @Getter
    private final String name = "ticket";

    @Getter @Setter
    private boolean start = false;

    private final ShardManager api;
    private final List<ListenerAdapter> listeners = new ArrayList<>();

    public PVChannelModule(ShardManager api) {
        this.api = api;
        listeners.add(new PVChannelListener(api));
    }

    @Override
    public boolean startUp() {
        listeners.forEach(api::addEventListener);
        return true;
    }

    @Override
    public boolean shutDown() {
        listeners.forEach(api::removeEventListener);
        return true;
    }

}
