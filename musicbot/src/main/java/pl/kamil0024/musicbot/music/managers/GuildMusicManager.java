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

package pl.kamil0024.musicbot.music.managers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.musicbot.music.managers.entity.AudioPlayerSendHandler;
import pl.kamil0024.musicbot.socket.SocketClient;
import pl.kamil0024.musicbot.socket.SocketRestAction;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GuildMusicManager extends AudioEventAdapter {

    @Getter public AudioPlayer player;
    @Getter public AudioManager audioManager;
    @Getter public AudioPlayerManager manager;
    @Getter public SocketClient socketClient;

    @Getter public final BlockingQueue<AudioTrack> queue;

    @Getter @Setter public AudioTrack aktualnaPiosenka = null;
    @Getter @Setter public Boolean destroy = false;

    public GuildMusicManager(AudioPlayerManager manager, AudioManager audioManager, SocketClient socketClient) {
        this.audioManager = audioManager;
        this.queue = new LinkedBlockingQueue<>();
        this.manager = manager;
        this.socketClient = socketClient;
        player = this.manager.createPlayer();
        player.addListener(this);
    }

    public AudioPlayerSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(player);
    }

    public void queue(AudioTrack track) {
        if (queue.size() > 10) return;

        if (!player.startTrack(track, true)) {
            queue.offer(track);
        }

    }

    @Nullable
    public AudioTrack nextTrack() {
        AudioTrack next = queue.poll();

        if (getDestroy() || next == null) {
            destroy();
            return null;
        }
        SocketRestAction action = new SocketRestAction(socketClient.api, socketClient.musicManager);
        socketClient.sendMessage(action.updateQueue());

        player.startTrack(next, false);
        setAktualnaPiosenka(next);
        return next;
    }

    public void destroy() {
        setDestroy(true);
        getPlayer().destroy();
        getPlayer().removeListener(this);
        getAudioManager().closeAudioConnection();
        setAktualnaPiosenka(null);
        getQueue().clear();
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            nextTrack();
        }
    }

}
