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

package pl.kamil0024.api.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.api.Response;
import pl.kamil0024.core.logger.Log;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class GuildChannelsHandler implements HttpHandler {

    private final ShardManager api;

    @Override
    public void handleRequest(HttpServerExchange ex) {
        if (!Response.checkIp(ex)) { return; }
        try {
            Map<String, String> channels = new HashMap<>();
            for (String s : new String[]{"425673488456482817", "494507499739676686", "502831202332573707", "506210855231291393"}) {
                Category cate = api.getCategoryById(s);
                if (cate == null) {
                    Log.newError("Kategoria " + s + " jest nullem!", getClass());
                    continue;
                }
                for (TextChannel c : cate.getTextChannels()) {
                    channels.put(c.getName(), c.getId());
                }
            }
            Response.sendObjectResponse(ex, channels);
        } catch (Exception e) {
            e.printStackTrace();
            Response.sendErrorResponse(ex, "Błąd!", e.getLocalizedMessage());
        }

    }

}