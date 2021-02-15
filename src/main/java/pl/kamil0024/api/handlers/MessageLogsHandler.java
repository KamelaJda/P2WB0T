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
import pl.kamil0024.api.Response;
import pl.kamil0024.core.database.DeletedMessagesDao;

@AllArgsConstructor
public class MessageLogsHandler implements HttpHandler {

    private final DeletedMessagesDao dao;

    @Override
    public void handleRequest(HttpServerExchange ex)  {
        if (!Response.checkIp(ex)) { return; }
        try {
            String type = ex.getQueryParameters().get("type").getFirst();
            String data = ex.getQueryParameters().get("data").getFirst();
            int offset = Integer.parseInt(ex.getQueryParameters().get("offset").getFirst());

            if (!type.equalsIgnoreCase("all") && !type.equalsIgnoreCase("channel") && !type.equalsIgnoreCase("user")) {
                Response.sendErrorResponse(ex, "Błąd!", "Typ " + type + " jest błędny!");
                return;
            }

            switch (type) {
                case "all":
                    Response.sendObjectResponse(ex, dao.getAll(offset));
                    break;
                case "channel":
                    Response.sendObjectResponse(ex, dao.getFromChannel(data, offset));
                    break;
                case "user":
                    Response.sendObjectResponse(ex, dao.getFromUser(data, offset));
            }

        } catch (Exception e) {
            e.printStackTrace();
            Response.sendErrorResponse(ex, "Błąd!", e.getLocalizedMessage());
        }
    }

}
