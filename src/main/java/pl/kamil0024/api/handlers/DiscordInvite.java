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

import com.google.inject.Inject;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import pl.kamil0024.api.APIModule;
import pl.kamil0024.api.Response;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.database.config.DiscordInviteConfig;

import java.util.Map;

public class DiscordInvite implements HttpHandler  {

    @Inject private final APIModule apiModule;

    public DiscordInvite(APIModule apiModule) {
        this.apiModule = apiModule;
    }

    @Override
    public void handleRequest(HttpServerExchange ex) {
        try {
            String token = ex.getQueryParameters().get("token").getFirst();
            if (!Ustawienia.instance.api.tokens.contains(token)) {
                Response.sendErrorResponse(ex, "Brak autoryzacji", "Token jest nieprawidłowy.");
                return;
            }
        } catch (Exception e) {
            Response.sendErrorResponse(ex, "Brak autoryzacji", "Token jest nieprawidłowy.");
            return;
        }

        String nick = ex.getQueryParameters().get("nick").getFirst();
        String ranga = ex.getQueryParameters().get("ranga").getFirst();
        String kod = ex.getQueryParameters().get("kod").getFirst();

        if (nick.isEmpty() || ranga.isEmpty() || kod.isEmpty()) {
            Response.sendErrorResponse(ex, "Pusty parametr", "Parametr nick,ranga,kod jest pusty.");
            return;
        }

        for (Map.Entry<String, DiscordInviteConfig> dcconfig : apiModule.getDcCache().asMap().entrySet()) {
            if (dcconfig.getValue().getNick().equalsIgnoreCase(nick)) {
                apiModule.getDcCache().invalidate(dcconfig.getKey());
            }
        }
        Response.sendResponse(ex, "Zapytanie przebiegło pomyślnie");
        apiModule.putDiscordConfig(nick, kod, ranga);
    }

}
