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

package pl.kamil0024.core.socket;

import com.google.common.eventbus.AsyncEventBus;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Data;
import pl.kamil0024.core.logger.Log;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class SocketServer {

    public final static Gson GSON = new Gson();

    private final AsyncEventBus eventBus;
    private final SocketManager socketManager;

    public SocketServer(AsyncEventBus eventBus, SocketManager socketManager) {
        this.socketManager = socketManager;
        this.eventBus = eventBus;
        eventBus.register(this);
    }

    public void start() {
        ServerSocket serverSocket;

        try {
            serverSocket = new ServerSocket(7070);
        } catch (Exception e) {
            Log.newError("Nie udało się wystartować servera z socketem", getClass());
            Log.newError(e, getClass());
            return;
        }

        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    int lastId = 0;
                    for (Map.Entry<Integer, SocketClient> entry : socketManager.getClients().entrySet()) {
                        lastId = entry.getKey();
                    }
                    SocketClient client = new SocketClient(socket, eventBus, lastId + 1);
                    client.start();
                    socketManager.getClients().put(client.getSocketId(), client);
                    Log.debug("Podłączono nowy socket!");
                } catch (Exception e) {
                    Log.newError(e, getClass());
                }
            }
        }).start();

    }

    @Data
    @AllArgsConstructor
    public static class SocketJson {
        private final int id;
        private final String json;
    }

    @Data
    @AllArgsConstructor
    public static class SocketDisconnect {
        private final int id;
    }

}
