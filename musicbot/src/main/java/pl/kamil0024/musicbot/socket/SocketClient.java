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
import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.musicbot.core.Ustawienia;
import pl.kamil0024.musicbot.core.logger.Log;
import pl.kamil0024.musicbot.music.managers.MusicManager;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class SocketClient extends Thread {

    private final static Gson GSON = new Gson();

    Socket socket;
    OutputStream output;
    PrintWriter writer;

    private final MusicManager musicManager;
    private final ShardManager api;

    public SocketClient(MusicManager musicManager, ShardManager api) {
        this.musicManager = musicManager;
        this.api = api;
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
                   writer.println("setBotId: " + Ustawienia.instance.bot.botId);
                   break;
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
        SocketAction socketAction = GSON.fromJson(msg, SocketAction.class);

        SocketRestAction action = new SocketRestAction(api, musicManager);
        Response response = null;

        if (socketAction.getTopic().equals("disconnect")) response = action.disconnect();
        if (socketAction.getTopic().equals("connect")) response = action.connect((String) socketAction.getArgs().get("channelId"));

        if (response != null) {
            response.setAction(socketAction);
            sendMessage(response);
        }

    }

    public void sendMessage(Response response) {
        writer.println(GSON.toJson(response));
    }

    @Data
    public static class SocketAction {
        public SocketAction() { }

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

}
