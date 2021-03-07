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
import lombok.Data;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.api.Response;
import pl.kamil0024.core.database.config.UserinfoConfig;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.status.StatusModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class StatusyGetHandler implements HttpHandler {

    private final StatusModule statusModule;
    private final ShardManager api;

    @Override
    public void handleRequest(HttpServerExchange ex) {
        if (!Response.checkIp(ex)) return;

        List<Encoder> lista = new ArrayList<>();
        for (Map.Entry<String, String> entry : statusModule.cache.asMap().entrySet()) {
            String key = entry.getKey().split("::String:")[1];
            Encoder encoder = new Encoder();
            encoder.setMember(MemberHistoryHandler.getWhateverConfig(key, api));
            encoder.setStatus(entry.getValue());
            lista.add(encoder);
        }

        Response.sendObjectResponse(ex, lista);

    }

    @Data
    private static class Encoder {
        public Encoder() { }
        private UserinfoConfig member;
        private String status;
    }

}
