package pl.kamil0024.privatechannel;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.socket.SocketManager;
import pl.kamil0024.privatechannel.listeners.PVChannelListener;

public class PVChannelModule implements Modul {

    @Getter
    private final String name = "pvchannels";

    @Getter @Setter
    private boolean start = false;

    private final ShardManager api;
    private final PVChannelListener listener;

    public PVChannelModule(ShardManager api, SocketManager socketManager) {
        this.api = api;
        listener = new PVChannelListener(api, socketManager);
    }

    @Override
    public boolean startUp() {
        api.addEventListener(listener);
        return true;
    }

    @Override
    public boolean shutDown() {
        api.removeEventListener(listener);
        return true;
    }

}
