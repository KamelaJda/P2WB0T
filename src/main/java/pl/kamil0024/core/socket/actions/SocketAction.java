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

package pl.kamil0024.core.socket.actions;

import pl.kamil0024.core.socket.SocketServer;

import java.util.HashMap;
import java.util.Map;

public interface SocketAction {

    Boolean getSendMessage();

    String getMemberId();

    String getChannelId();

    String getTopic();

    int getSocketId();

    void setSendMessage(Boolean bol);

    Integer getActionID();
    void setActionID(Integer id);

    default Map<String, Object> getArgs() {
        return new HashMap<>();
    }

    default String toJson() {
        Map<String, Object> map = new HashMap<>();
        map.put("memberId", getMemberId());
        map.put("channelId", getChannelId());
        map.put("topic", getTopic());
        map.put("socketId", getSocketId());
        map.put("args", getArgs());
        map.put("sendMessage", getSendMessage());
        map.put("actionID", getActionID());
        return SocketServer.GSON.toJson(map);
    }

}
