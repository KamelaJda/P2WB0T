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

package pl.kamil0024.core.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import pl.kamil0024.core.Ustawienia;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum DiscordRank {

    GRACZ(Ustawienia.instance.rangi.gracz),
    VIP(Ustawienia.instance.rangi.vip),
    VIPPLUS(Ustawienia.instance.rangi.vipplus),
    MVP(Ustawienia.instance.rangi.mvp),
    MVPPLUS(Ustawienia.instance.rangi.mvpplus),
    MVPPLUSPLUS(Ustawienia.instance.rangi.mvpplusplus),
    SPONSOR(Ustawienia.instance.rangi.sponsor),
    MINIYT(Ustawienia.instance.rangi.miniyt),
    YT(Ustawienia.instance.rangi.yt),
    BUILDTEAM(Ustawienia.instance.rangi.buildteam),
    STAZYSTA(Ustawienia.instance.rangi.stazysta),
    EKIPA(Ustawienia.instance.rangi.ekipa),
    POMOCNIK(Ustawienia.instance.rangi.pomocnik),
    MODERATOR(Ustawienia.instance.rangi.moderator),
    OWNER(Ustawienia.instance.rangi.korona),
    CHATMOD(Ustawienia.instance.roles.chatMod),
    ADMINISTRATOR(Ustawienia.instance.rangi.administrator);

    private final String roleId;

    public static ArrayList<DiscordRank> getRanks(Member member) {
        ArrayList<DiscordRank> rank = new ArrayList<>();
        List<String> rolesId = member.getRoles().stream().map(Role::getId).collect(Collectors.toList());
        for (DiscordRank value : DiscordRank.values()) {
            if (rolesId.contains(value.getRoleId())) {
                rank.add(value);
            }
        }
        return rank;
    }

}
