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

package pl.kamil0024.musicbot.socket;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.musicbot.api.handlers.Connect;
import pl.kamil0024.musicbot.api.handlers.QueueHandler;
import pl.kamil0024.musicbot.music.managers.GuildMusicManager;
import pl.kamil0024.musicbot.music.managers.MusicManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DuplicatedCode")
@AllArgsConstructor
public class SocketRestAction {

    private final ShardManager api;
    private final MusicManager musicManager;

    public SocketClient.Response disconnect() {
        SocketClient.Response response = new SocketClient.Response();
        response.setMessageType("message");

        Guild guild = Connect.getGuild(api);
        AudioManager state = guild.getAudioManager();
        if (state.getConnectedChannel() == null) {
            response.setSuccess(false);
            response.setErrorMessage("not nie jest na żadnym kanale głosowym (jakimś cudem)");
            return response;
        }
        GuildMusicManager manager = musicManager.getGuildAudioPlayer(Connect.getGuild(api));
        manager.destroy();
        state.closeAudioConnection();
        response.setSuccess(true);
        response.setData("bot opuścił kanał głosowy");
        return response;
    }

    public SocketClient.Response connect(String channelId) {
        SocketClient.Response response = new SocketClient.Response();
        response.setSuccess(false);
        response.setMessageType("message");
        VoiceChannel vc = api.getVoiceChannelById(channelId);

        if (vc == null) {
            response.setErrorMessage("Nie udało się znaleźć kanału o ID: " + channelId);
            return response;
        }

        try {
            Guild guild = Connect.getGuild(api);
            guild.getAudioManager().openAudioConnection(vc);
            response.setSuccess(true);
            response.setData("bot dołączył na kanał");
        } catch (InsufficientPermissionException e) {
            response.setData("bot nie ma wystarczających permisji");
        } catch (UnsupportedOperationException e) {
            response.setErrorMessage("Wystąpił wewnętrzny błąd w JDA:" + e.getLocalizedMessage());
        } catch (Exception e) {
            response.setErrorMessage("Wystąpił błąd:" + e.getLocalizedMessage());
        }
        return response;
    }

    public SocketClient.Response playingTrack() {
        SocketClient.Response response = new SocketClient.Response();
        AudioTrack track = musicManager.getGuildAudioPlayer(Connect.getGuild(api)).getPlayer().getPlayingTrack();
        if (track == null) {
            response.setMessageType("message");
            response.setSuccess(false);
            response.setErrorMessage("bot nie nie gra!");
            return response;
        }
        response.setMessageType("track");
        response.setSuccess(true);
        response.setData(new QueueHandler.Track(track));
        return response;
    }

    public SocketClient.Response queue() {
        SocketClient.Response response = new SocketClient.Response();
        GuildMusicManager manager = musicManager.getGuildAudioPlayer(Connect.getGuild(api));
        List<AudioTrack> klele = new ArrayList<>(manager.getQueue());

        if (klele.isEmpty() && manager.getPlayer().getPlayingTrack() == null) {
            response.setSuccess(false);
            response.setMessageType("message");
            response.setErrorMessage("kolejka jest pusta!");
            return response;
        }
        List<QueueHandler.Track> traki = new ArrayList<>();
        traki.add(0, new QueueHandler.Track(manager.getPlayer().getPlayingTrack()));
        klele.forEach(t -> traki.add(new QueueHandler.Track(t)));
        response.setSuccess(true);
        response.setMessageType("queuelist");
        response.setData(traki);
        return response;
    }

    public SocketClient.Response shutdown() {
        SocketClient.Response response = new SocketClient.Response();
        response.setMessageType("message");
        response.setData("zamykam");

        Guild guild = Connect.getGuild(api);
        AudioManager state = guild.getAudioManager();
        if (state.getConnectedChannel() != null) state.closeAudioConnection();
        Runnable task = () -> System.exit(0);
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ses.schedule(task, 10, TimeUnit.SECONDS);
        return response;
    }

    public SocketClient.Response skip() {
        SocketClient.Response response = new SocketClient.Response();
        response.setMessageType("message");
        response.setSuccess(false);
        Guild guild = Connect.getGuild(api);
        AudioManager state = guild.getAudioManager();
        if (state.getConnectedChannel() == null) {
            response.setErrorMessage("bot nie jest na żadnym kanale!");
            return response;
        }
        GuildMusicManager manager = musicManager.getGuildAudioPlayer(Connect.getGuild(api));
        if (manager.getPlayer().getPlayingTrack() == null) {
            response.setErrorMessage("bot aktualnie nic nie gra, a nie można pominąć niczego!");
            return response;
        }
        response.setSuccess(true);
        if (manager.nextTrack() == null) response.setData("kolejka się skończyła! Opuszczam kanał");
        else response.setData("puszczam następną piosenkę");
        return response;
    }

}
