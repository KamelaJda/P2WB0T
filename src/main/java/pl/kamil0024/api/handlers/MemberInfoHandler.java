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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.api.Response;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.util.UserUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class MemberInfoHandler implements HttpHandler {

    private final ShardManager api;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void handleRequest(HttpServerExchange ex) {
        if (!Response.checkIp(ex)) { return; }

        try {
            MemberInfo memberInfo = new MemberInfo();
            Member mem = api.getGuildById(Ustawienia.instance.bot.guildId)
                    .getMemberById(ex.getQueryParameters().get("id").getFirst());

            if (mem != null) {
                memberInfo = getMember(mem);
            } else memberInfo.setInGuild(false);

            Response.sendObjectResponse(ex, memberInfo);

        } catch (Exception e) {
            e.printStackTrace();
            Response.sendErrorResponse(ex, "Błąd!", "Nie udało się wykonać requesta!");
        }

    }

    @Data
    public static class MemberInfo {
        public MemberInfo() { }

        private String username;
        private String nickname;
        private String avatarUrl;
        private boolean inGuild = true;
        private PermLevel permLevel;
        private List<String> roles = new ArrayList<>();

    }

    public static MemberInfo getMember(Member member) {
        MemberInfo inf = new MemberInfo();
        inf.setUsername(UserUtil.getName(member.getUser()));
        inf.setAvatarUrl(member.getUser().getAvatarUrl());
        if (member.getNickname() != null) inf.setNickname(member.getNickname());
        inf.setPermLevel(UserUtil.getPermLevel(member));
        inf.setRoles(member.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
        return inf;
    }

    public static MemberInfo getUser(User user) {
        MemberInfo inf = new MemberInfo();
        inf.setUsername(UserUtil.getName(user));
        inf.setAvatarUrl(user.getAvatarUrl());
        inf.setInGuild(false);
        return inf;
    }

}