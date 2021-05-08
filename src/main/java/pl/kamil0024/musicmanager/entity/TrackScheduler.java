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

package pl.kamil0024.musicmanager.entity;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.managers.AudioManager;
import pl.kamil0024.music.commands.QueueCommand;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler extends AudioEventAdapter {
    // hahaha @Data robi brrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr

    @Getter
    private final AudioPlayer player;
    @Getter
    private final AudioManager audioManager;
    @Getter
    private final AudioPlayerManager manager;
    @Getter
    private final GuildMusicManager guildMusicManager;

    @Getter
    public final BlockingQueue<AudioTrack> queue;

    @Getter
    @Setter
    public AudioTrack aktualnaPiosenka = null;
    @Getter
    @Setter
    public Boolean destroy = false;
    @Getter
    @Setter
    public Boolean loop = false;

    public TrackScheduler(AudioPlayer player, AudioManager audioManager, AudioPlayerManager manager, GuildMusicManager guildMusicManager) {
        this.player = player;
        this.audioManager = audioManager;
        this.manager = manager;
        this.queue = new LinkedBlockingQueue<>();
        this.guildMusicManager = guildMusicManager;
    }

    public void queue(AudioTrack track) {
        if (!player.startTrack(track, true)) {
            queue.offer(track);
        }
    }

    public void nextTrack() {
        AudioTrack next = null;
        if (!getLoop()) {
            next = queue.poll();
        }

        if (getDestroy() || (next == null && !getLoop())) {
            destroy();
            return;
        }

        if (getLoop()) {
            manager.loadItemOrdered(guildMusicManager, QueueCommand.getYtLink(getAktualnaPiosenka()), new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    setAktualnaPiosenka(track);
                    player.startTrack(track, false);
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                }

                @Override
                public void noMatches() {
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                }

            });
            return;
        }

        if (next != null) {
            player.startTrack(next, false);
            setAktualnaPiosenka(next);
        }

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