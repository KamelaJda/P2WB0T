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
import org.json.JSONObject;
import pl.kamil0024.api.Response;
import pl.kamil0024.embedgenerator.entity.EmbedRedisManager;

import java.util.Random;

public class EmbedHandler implements HttpHandler {

    private final EmbedRedisManager embedRedisManager;
    private final Random radom = new Random(); // radom - tak

    public EmbedHandler(EmbedRedisManager embedRedisManager) {
        this.embedRedisManager = embedRedisManager;
    }

    @Override
    public void handleRequest(HttpServerExchange ex) {
        if (!Response.checkIp(ex)) { return; }
        try {
            JSONObject json = new JSONObject(Response.getBody(ex.getInputStream()));
            int rand = radom.nextInt(10000);
            embedRedisManager.save(String.valueOf(rand), json.toString());
            Response.sendObjectResponse(ex, new Code(rand));
        } catch (Exception e) {
            Response.sendErrorResponse(ex, "Błąd", "Nie udało się wysłać requesta! " + e.getMessage());
        }
    }

    @Data
    @AllArgsConstructor
    private static class Code {
        private final int code;
    }

}
