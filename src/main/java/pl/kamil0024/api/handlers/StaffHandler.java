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
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.api.Response;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.util.UserUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@AllArgsConstructor
public class StaffHandler implements HttpHandler {

    private final ShardManager api;

    // 

    @Override
    public void handleRequest(HttpServerExchange ex){
        if (!Response.checkToken(ex)) return;

        Guild g = api.getGuildById(Ustawienia.instance.bot.guildId);
        StaffList staffList = new StaffList();

        List<Member> administracja = Objects.requireNonNull(g).getMembersWithRoles(api.getRoleById(Ustawienia.instance.rangi.ekipa));

        for (Member zarzad : g.getMembersWithRoles(api.getRoleById(Ustawienia.instance.rangi.korona))) {
            staffList.getZarzad().add(getOsoba(zarzad));
        }

        for (Member member : administracja) {
            PermLevel plvl = UserUtil.getPermLevel(member);
            switch (plvl) {
                case ADMINISTRATOR:
                    staffList.getAdministratorzy().add(getOsoba(member));
                    break;
                case MODERATOR:
                    staffList.getModeratorzy().add(getOsoba(member));
                    break;
                case HELPER:
                    staffList.getPomocnicy().add(getOsoba(member));
                    break;
                case STAZYSTA:
                    staffList.getStazysci().add(getOsoba(member));
                    break;
                case DEVELOPER:
                    String s = UserUtil.getPrefix(member);
                    if (s == null || member.getRoles().contains(api.getRoleById(Ustawienia.instance.rangi.korona))) break;
                    Osoba o = getOsoba(member);
                    switch (s) {
                        case "[POM]":
                            staffList.getStazysci().add(o);
                            break;
                        case "[MOD]":
                            staffList.getModeratorzy().add(o);
                            break;
                        case "[ADM]":
                            staffList.getAdministratorzy().add(o);
                            break;
                    }
                    break;
            }
        }

        Response.sendObjectResponse(ex, staffList);

    }

    private Osoba getOsoba(Member mem) {
        return new Osoba(UserUtil.getMcNick(mem), UserUtil.getName(mem.getUser()), UserUtil.getPrefix(mem), getZespoly(mem), getLiderList(mem));
    }

    private List<String> getZespoly(Member mem) {
        List<String> zespoly = new ArrayList<>();
        for (Role role : mem.getRoles()) {
            if (Ustawienia.instance.zespoly.zespoly.contains(role.getId())) {
                zespoly.add(role.getName());
            }
        }
        return zespoly;
    }

    private List<String> getLiderList(Member mem) {
        return Ustawienia.instance.zespoly.liderzy.getOrDefault(mem.getId(), new ArrayList<>());
    }

    @Data
    private static class StaffList {

        private List<Osoba> zarzad = new ArrayList<>();
        private List<Osoba> administratorzy = new ArrayList<>();
        private List<Osoba> moderatorzy = new ArrayList<>();
        private List<Osoba> pomocnicy = new ArrayList<>();
        private List<Osoba> stazysci = new ArrayList<>();

    }

    @Data
    @AllArgsConstructor
    private static class Osoba {
        private final String nick;
        private final String discordnick;
        private final String prefix;
        private final List<String> zespoly;
        private final List<String> lider;
    }

}
