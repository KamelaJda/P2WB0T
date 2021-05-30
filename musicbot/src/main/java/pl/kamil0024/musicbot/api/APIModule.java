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

package pl.kamil0024.musicbot.api;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.musicbot.api.listeners.LeaveVcListener;
import pl.kamil0024.musicbot.core.Ustawienia;
import pl.kamil0024.musicbot.core.module.Modul;
import pl.kamil0024.musicbot.core.util.EventWaiter;
import pl.kamil0024.musicbot.music.managers.MusicManager;
import pl.kamil0024.musicbot.socket.SocketClient;

@Getter
public class APIModule implements Modul {

    private final ShardManager api;
    private final MusicManager musicManager;
    private final EventWaiter eventWaiter;
    private final SocketClient client;

    @Getter
    private final String name = "api";

    @Getter @Setter
    private boolean start = false;

    private final Guild guild;

    private LeaveVcListener leaveVcListener;

    public APIModule(ShardManager api, MusicManager musicManager, EventWaiter eventWaiter) {
        this.api = api;
        this.guild = api.getGuildById(Ustawienia.instance.bot.guildId);
        if (guild == null) throw new UnsupportedOperationException("Gildia docelowa jest nullem!");
        this.musicManager = musicManager;
        this.eventWaiter = eventWaiter;
        this.client = musicManager.socketClient;
    }

    @Override
    public boolean startUp() {
        this.leaveVcListener = new LeaveVcListener(musicManager, eventWaiter, client);
        api.addEventListener(leaveVcListener);
        return true;
    }

    @Override
    public boolean shutDown() {
        api.removeEventListener(this.leaveVcListener);
        return true;
    }

}
