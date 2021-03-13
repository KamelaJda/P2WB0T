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
import net.dv8tion.jda.api.sharding.ShardManager;
import org.json.JSONObject;
import pl.kamil0024.api.APIModule;
import pl.kamil0024.api.Response;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.database.config.DiscordInviteConfig;

import java.util.Objects;

@AllArgsConstructor
public class WeryfikacjaJoinHandler implements HttpHandler {

    private final APIModule apiModule;
    private final ShardManager api;

    @Override
    public void handleRequest(HttpServerExchange ex) {
        if (!Response.checkIp(ex)) return;

        try {
            JSONObject json = new JSONObject(Response.getBody(ex.getInputStream()));

            String kod = json.getString("kod");
            String user = json.getString("user");
            String token = json.getString("token");

            DiscordInviteConfig conf = apiModule.getDiscordConfig(kod);
            if (conf == null) {
                Response.sendErrorResponse(ex, "Błąd!", "Podany kod weryfikacji jest błędny! Spróbuj ponownie lub wygeneruj nowy.");
                return;
            }

            try {
                Objects.requireNonNull(api.getGuildById(Ustawienia.instance.bot.guildId)).addMember(token, user).complete();
            } catch (Exception e) {
                if (!e.getMessage().equals("User is already in this guild")) {
                    Response.sendErrorResponse(ex, "Błąd!", "Nie udało się dodać Ciebie na serwer! Spróbuj zalogować się ponownie.");
                    return;
                }
            }

            Response.sendResponse(ex, "Akcja przebiegła pomyślnie!");
            apiModule.getNewWery().put(user, conf);
            apiModule.getDcCache().invalidate(kod);

        } catch (Exception e) {
            e.printStackTrace();
            Response.sendErrorResponse(ex, "Błąd!", "Wystąpił błąd ze strony serwera! Spróbuj ponownie lub zawiadom adminsitracje.");
        }

    }

}
