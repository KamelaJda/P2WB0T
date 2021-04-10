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

package pl.kamil0024.music;


import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeSearchProvider;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandManager;
import pl.kamil0024.core.database.VoiceStateDao;
import pl.kamil0024.core.database.config.VoiceStateConfig;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.socket.SocketManager;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.music.commands.*;
import pl.kamil0024.music.commands.privates.*;
import pl.kamil0024.musicmanager.entity.GuildMusicManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MusicModule implements Modul {

    private ArrayList<Command> cmd;

    CommandManager commandManager;
    ShardManager api;
    EventWaiter eventWaiter;
    VoiceStateDao voiceStateDao;

    @Getter @Setter
    private boolean start = false;

    public final DefaultAudioPlayerManager defaultAudioPlayerManager = new DefaultAudioPlayerManager();
    public final AudioPlayerManager playerManager;
    @Getter public final Map<Long, GuildMusicManager> musicManagers;

    private static final YoutubeSearchProvider youtubeSearchProvider = new YoutubeSearchProvider();
    public YoutubeAudioSourceManager youtubeSourceManager;
    public SocketManager socketManager;

    public MusicModule(CommandManager commandManager, ShardManager api, EventWaiter eventWaiter, VoiceStateDao voiceStateDao, SocketManager socketManager) {
        this.commandManager = commandManager;
        this.api = api;
        this.eventWaiter = eventWaiter;
        this.voiceStateDao = voiceStateDao;
        this.socketManager = socketManager;

        this.playerManager = defaultAudioPlayerManager;
        this.musicManagers = new HashMap<>();
        this.youtubeSourceManager = new YoutubeAudioSourceManager(true);
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        playerManager.registerSourceManager(youtubeSourceManager);
        playerManager.registerSourceManager(new BeamAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());

        VoiceStateConfig vsc = voiceStateDao.get("1");
        if (vsc != null && vsc.getVoiceChannel() != null) {
            VoiceChannel vc = api.getVoiceChannelById(vsc.getVoiceChannel());
            TextChannel txt = api.getTextChannelById(Ustawienia.instance.channel.moddc);
            if (txt != null) {
                loadAndPlay(txt, vsc.getAktualnaPiosenka(), vc, false);
                vsc.getQueue().forEach(p -> loadAndPlay(txt, p, vc, false));
            }
            voiceStateDao.delete("1");
        }

    }

    @Override
    public boolean startUp() {
        cmd = new ArrayList<>();

        cmd.add(new PlayCommand(this));
        cmd.add(new QueueCommand(this, eventWaiter));
        cmd.add(new VolumeCommand(this));
        cmd.add(new ResumeCommand(this));
        cmd.add(new SkipCommand(this));
        cmd.add(new YouTubeCommand(this, eventWaiter));
        cmd.add(new LeaveCommand(this));
        cmd.add(new LoopCommand(this));

        //#region Prywatne
        cmd.add(new PrivatePlayCommand(socketManager));
        cmd.add(new PrivateLeaveCommand(socketManager));
        cmd.add(new PrivateQueueCommand(socketManager));
        cmd.add(new PrivateSkipCommand(socketManager));
        cmd.add(new PrivateYouTubeCommand(socketManager, eventWaiter, this));
        //#endregion Prywatne

        cmd.forEach(commandManager::registerCommand);
        setStart(true);
        return true;
    }

    @Override
    public boolean shutDown() {
        commandManager.unregisterCommands(cmd);
        setStart(false);
        return true;
    }

    @Override
    public String getName() {
        return "music";
    }
    
    public synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = guild.getIdLong();
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null || musicManager.getPlayer() == null || musicManager.getScheduler().getDestroy()) {
            musicManager = new GuildMusicManager(playerManager, guild.getAudioManager());
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    public boolean loadAndPlay(final TextChannel channel, final String trackUrl, VoiceChannel vc, boolean sendMsg) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        AtomicBoolean error = new AtomicBoolean(false);

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if (sendMsg) channel.sendMessage("Dodaje do kolejki " + track.getInfo().title).queue();
                play(channel.getGuild(), musicManager, track, vc);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (sendMsg) channel.sendMessage("Dodaje do kolejki " +  playlist.getTracks().size() + " piosenek").queue();
                for (AudioTrack track : playlist.getTracks()) {
                    play(channel.getGuild(), musicManager, track, vc);
                }
            }

            @Override
            public void noMatches() {
                if (sendMsg) channel.sendMessage("Nie znaleziono dopasowań!").queue();
                error.set(true);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Nie można odtworzyć piosenki: " + exception.getMessage()).queue();
                error.set(true);
            }
        });
        return !error.get();
    }

    public boolean loadAndPlay(final TextChannel channel, final String trackUrl, VoiceChannel vc) {
        return loadAndPlay(channel, trackUrl, vc, true);
    }

    public synchronized void play(Guild guild, GuildMusicManager musicManager, AudioTrack track, VoiceChannel vc) {
        for (AudioTrack audioTrack : musicManager.scheduler.getQueue()) {
            if (audioTrack.getIdentifier().equals(track.getIdentifier())) {
                return;
            }
        }

        connectToFirstVoiceChannel(guild.getAudioManager(), vc);
        musicManager.scheduler.queue(track);

        if (musicManager.scheduler.getAktualnaPiosenka() == null) {
            musicManager.scheduler.setAktualnaPiosenka(track);
        }

    }

    public static void connectToFirstVoiceChannel(AudioManager audioManager, VoiceChannel vc) {
        audioManager.openAudioConnection(vc);
    }

    public List<AudioTrack> search(String tytul) {
        List<AudioTrack> results = new ArrayList<>();
        AudioItem playlist = youtubeSearchProvider.loadSearchResult(tytul, info -> new YoutubeAudioTrack(info, youtubeSourceManager));

        if (playlist instanceof AudioPlaylist) {
            results.addAll(((AudioPlaylist) playlist).getTracks());
        }

        return results;
    }

    public void load() {
        try {
            Guild g = api.getGuildById(Ustawienia.instance.bot.guildId);
            if (g == null) return;

            VoiceStateConfig vsc = new VoiceStateConfig("1");
            if (g.getAudioManager().getConnectedChannel() == null) return;
            vsc.setVoiceChannel(g.getAudioManager().getConnectedChannel().getId());

            ArrayList<String> linki = new ArrayList<>();
            for (AudioTrack audioTrack : getMusicManagers().get(g.getIdLong()).getScheduler().getQueue()) {
                linki.add(QueueCommand.getYtLink(audioTrack));
            }

            vsc.setQueue(linki);
            vsc.setAktualnaPiosenka(QueueCommand.getYtLink(getMusicManagers().get(g.getIdLong()).getPlayer().getPlayingTrack()));
            voiceStateDao.save(vsc);
        } catch (Exception ignored) {}
    }

}
