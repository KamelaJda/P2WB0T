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

import com.google.gson.Gson;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.musicbot.api.handlers.Connect;
import pl.kamil0024.musicbot.api.handlers.QueueHandler;
import pl.kamil0024.musicbot.core.Ustawienia;
import pl.kamil0024.musicbot.core.logger.Log;
import pl.kamil0024.musicbot.music.managers.GuildMusicManager;
import pl.kamil0024.musicbot.music.managers.MusicManager;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SocketClient extends Thread {

    private final static Gson GSON = new Gson();

    Socket socket = null;
    OutputStream output;
    PrintWriter writer;

    public final MusicManager musicManager;
    public final ShardManager api;

    public SocketClient(MusicManager musicManager, ShardManager api) {
        this.musicManager = musicManager;
        this.api = api;
        LeaveListener listener = new LeaveListener();
        api.addEventListener(listener);
    }

    @Override
    public void start() {

       try {
           while (true) {
               Thread.sleep(10000);

               try {
                   socket = new Socket("localhost", 7070);
                   output = socket.getOutputStream();
                   writer = new PrintWriter(output, true);
                   sendMessage("setBotId: " + Ustawienia.instance.bot.botId);
                   break;
               } catch(UnknownHostException ignored) {
               } catch (Exception e) {
                   Log.error("Nie udało się połączyć z serwerem!");
                   e.printStackTrace();
               }
           }

           new Thread(() -> {
               try {
                   InputStream input = socket.getInputStream();
                   BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                   String text;
                   while ((text = reader.readLine()) != null) {
                       retrieveMessage(text);
                   }
               } catch (Exception e) {
                   e.printStackTrace();
               }
           }).start();

       } catch (Exception e) {
           e.printStackTrace();
       }

    }

    public void retrieveMessage(String msg) {
        Log.debug("Nowa wiadomość od serwera: " + msg);
        SocketAction socketAction = GSON.fromJson(msg, SocketAction.class);

        SocketRestAction action = new SocketRestAction(api, musicManager);
        Response response = null;

        if (socketAction.getTopic().equals("play")) {
            response = new Response();
            response.setMessageType("message");
            response.setSuccess(false);

            Guild guild = Connect.getGuild(api);
            AudioManager state = null;
            boolean inChannel = false;

            for (int i = 0; i < 5; i++) {
                state = guild.getAudioManager();
                if (state.getConnectedChannel() == null) {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                        break;
                    }
                } else {
                    inChannel = true;
                    break;
                }
            }

            if (!inChannel) {
                response.setSuccess(false);
                response.setErrorMessage("bot nie jest na żadnym kanale!");
                sendMessage(response);
                return;
            }

            final VoiceChannel vc = state.getConnectedChannel();

            GuildMusicManager serwerManager = musicManager.getGuildAudioPlayer(guild);
            serwerManager.getManager().loadItemOrdered(serwerManager, (String) socketAction.getArgs().get("track"), new AudioLoadResultHandler() {

                @Override
                public void trackLoaded(AudioTrack track) {
                    musicManager.play(guild, serwerManager, track, vc);
                    List<String> traki = new ArrayList<>();
                    traki.add(0, serwerManager.getPlayer().getPlayingTrack().getIdentifier());
                    serwerManager.getQueue().stream().map(AudioTrack::getIdentifier).forEach(traki::add);

                    List<Object> obj = new ArrayList<>();
                    obj.add(new QueueHandler.Track(track));
                    obj.add(traki);
                    Response r = new Response(socketAction, true, "embedtrack", null, obj);
                    sendMessage(r);
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    for (AudioTrack track : playlist.getTracks()) {
                        musicManager.play(guild, serwerManager, track, vc);
                    }
                    Response r = new Response(socketAction, true, "message", null, "dodano " + playlist.getTracks().size() + " piosenek do kolejki (max. limit w kolejce to **10**).");
                    sendMessage(r);
                }

                @Override
                public void noMatches() {
                    Response r = new Response(socketAction, false, "message", "nie znaleziono dopasowań", null);
                    sendMessage(r);
                    if (serwerManager.getPlayer().getPlayingTrack() == null) serwerManager.destroy();
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    Response r = new Response(socketAction, false, "message", "nie udało się dodać piosenki do kolejki! Error: " + exception.getLocalizedMessage(), null);
                    sendMessage(r);
                    if (serwerManager.getPlayer().getPlayingTrack() == null) serwerManager.destroy();
                }
            });

            return;
        }

        try {
            switch (socketAction.getTopic()) {
                case "disconnect":
                    response = action.disconnect();
                    break;
                case "connect":
                    String ch = (String) socketAction.getArgs().get("voiceChannel");
                    response = action.connect(ch);
                    break;
                case "playingtrack":
                    response = action.playingTrack();
                    break;
                case "queue":
                    response = action.queue();
                    break;
                case "shutdown":
                    response = action.shutdown();
                    break;
                case "skip":
                    response = action.skip();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            response = new Response();
            response.setAction(socketAction);
            response.setMessageType("message");
            response.setSuccess(false);
            response.setErrorMessage("Wystąpił błąd podczas wysyłania requesta do socketa. Error: " + e.getLocalizedMessage());
        }

        if (response != null) {
            response.setAction(socketAction);
            Log.debug("response: " + GSON.toJson(response));
            sendMessage(response);
        }

    }

    public void sendMessage(Response response) {
        writer.println(GSON.toJson(response));
    }

    public void sendMessage(String msg) {
        writer.println(msg);
    }

    @Data
    @AllArgsConstructor
    public static class SocketAction {
        private Boolean sendMessage;
        private String memberId;
        private String channelId;
        private String topic;
        private int socketId;
        private Map<String, Object> args;

    }

    @Data
    @AllArgsConstructor
    public static class Response {
        public Response() { }

        private SocketAction action;
        private boolean success;
        private String messageType;
        private String errorMessage;
        private Object data;

    }

    private class LeaveListener extends ListenerAdapter {

        @Override
        public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
            if (event.getEntity().getId().equals(event.getGuild().getSelfMember().getId())) {
                sendMessage("setChannel: null");
            }
        }

        @Override
        public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
            if (event.getEntity().getId().equals(event.getGuild().getSelfMember().getId())) {
                sendMessage("setChannel: " + event.getChannelJoined().getId());
            }
        }

    }

}
