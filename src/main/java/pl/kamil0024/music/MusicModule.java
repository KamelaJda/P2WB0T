package pl.kamil0024.music;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeSearchProvider;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandManager;
import pl.kamil0024.core.database.VoiceStateDao;
import pl.kamil0024.core.database.config.VoiceStateConfig;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.music.commands.*;
import pl.kamil0024.musicmanager.entity.GuildMusicManager;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MusicModule implements Modul {

    private ArrayList<Command> cmd;

    @Inject CommandManager commandManager;
    @Inject ShardManager api;
    @Inject EventWaiter eventWaiter;
    @Inject VoiceStateDao voiceStateDao;

    private boolean start = false;

    private final AudioPlayerManager playerManager;
    @Getter public final Map<Long, GuildMusicManager> musicManagers;

    private static YoutubeSearchProvider youtubeSearchProvider = new YoutubeSearchProvider();

    public YoutubeAudioSourceManager youtubeSourceManager;

    public MusicModule(CommandManager commandManager, ShardManager api, EventWaiter eventWaiter, VoiceStateDao voiceStateDao) {
        this.commandManager = commandManager;
        this.api = api;
        this.eventWaiter = eventWaiter;
        this.voiceStateDao = voiceStateDao;

        this.playerManager = new DefaultAudioPlayerManager();
        this.musicManagers = new HashMap<>();
        this.youtubeSourceManager = new YoutubeAudioSourceManager(true);
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        playerManager.registerSourceManager(youtubeSourceManager);

        VoiceStateConfig vsc = voiceStateDao.get("1");
        if (vsc != null && vsc.getVoiceChannel() != null) {
            VoiceChannel vc = api.getVoiceChannelById(vsc.getVoiceChannel());
            TextChannel txt = api.getTextChannelById(Ustawienia.instance.channel.moddc);
            loadAndPlay(txt, vsc.getAktualnaPiosenka(), vc, false);
            vsc.getQueue().forEach(p -> loadAndPlay(txt, p, vc, false));
            voiceStateDao.delete();
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

    @Override
    public boolean isStart() {
        return start;
    }

    @Override
    public void setStart(boolean bol) {
        this.start = bol;
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
                if (sendMsg) channel.sendMessage("Dodaje do kolejki " +  playlist.getTracks() + "piosenek").queue();
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
