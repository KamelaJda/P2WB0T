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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.json.JSONObject;
import pl.kamil0024.api.Response;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.util.GsonUtil;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.core.util.kary.Dowod;
import pl.kamil0024.core.util.kary.KaryJSON;
import pl.kamil0024.moderation.commands.PunishCommand;
import pl.kamil0024.moderation.listeners.ModLog;
import pl.kamil0024.stats.StatsModule;
import pl.kamil0024.status.StatusModule;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class CasePuyHandler implements HttpHandler {

    private final StatusModule statusModule;
    private final KaryJSON karyJSON;
    private final ModLog modLog;
    private final StatsModule statsModule;
    private final ShardManager api;
    private final CaseDao caseDao;

    @Override
    public void handleRequest(HttpServerExchange ex) {
        if (!Response.checkIp(ex)) return;

        try {
            JSONObject json = new JSONObject(Response.getBody(ex.getInputStream()));
            Deserializer deserializer = GsonUtil.fromJSON(json.toString(), Deserializer.class);

            try {
                Guild g = api.getGuildById(Ustawienia.instance.bot.guildId);
                Member adm = g.getMemberById(deserializer.getAdm());
                if (adm == null) throw new Exception("Nie ma użytkownika '" + deserializer.getAdm() + "' na serwerze!");

                if (UserUtil.getPermLevel(adm).getNumer() < PermLevel.CHATMOD.getNumer()) {
                    throw new Exception("Nie masz wystarczających uprawnień!");
                }

                KaryJSON.Kara kara = karyJSON.getByName(deserializer.getPowod());
                if (kara == null) throw new Exception("Powód kary jet zły!");

                for (Deserializer.Karana o : deserializer.getKarani()) {
                    Member mem = g.getMemberById(o.id);
                    if (mem == null || UserUtil.getPermLevel(adm).getNumer() <= UserUtil.getPermLevel(mem).getNumer()) {
                        continue;
                    }

                    Dowod dowod = new Dowod(1, adm.getId(), "Wystawione automatycznie przez zgłoszenie na stronie. Treść statusu poniżej.\n" + o.getStatus(), null);
                    statusModule.cache.invalidate(mem.getId());
                    PunishCommand.putPun(kara, Collections.singletonList(mem), adm, null, caseDao, modLog, statsModule, dowod, null);
                }

                Response.sendResponse(ex, "Zadanie przebiegło pomyślnie");

            } catch (Exception e) {
                Response.sendErrorResponse(ex, "Błąd!", e.getLocalizedMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            Response.sendErrorResponse(ex, "Bład!", "Wystąpił błąd z requestem. " + e.getLocalizedMessage());
        }

    }

    @Data
    @AllArgsConstructor
    private static class Deserializer {
        public Deserializer() {
        }

        private String adm;
        private String powod;
        private List<Karana> karani;

        @Data
        private static class Karana {
            public String id;
            public String status;
        }

    }

}
