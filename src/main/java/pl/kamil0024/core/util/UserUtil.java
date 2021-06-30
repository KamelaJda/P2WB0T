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

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.enums.PermLevel;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class UserUtil {

    public static boolean isOwner(User user) {
        List<String> xd = Ustawienia.instance.devs;
        for (String id : xd) {
            if (user.getId().equals(id)) return true;
        }
        return false;
    }

    public static PermLevel getPermLevel(User user) {
        Guild g = user.getJDA().getGuildById(Ustawienia.instance.bot.guildId);
        if (g == null) throw new IllegalArgumentException("Ustawienia.instance.bot.guildId == null");
        try {
            Member mem = g.retrieveMemberById(user.getId()).complete();
            return getPermLevel(mem);
        } catch (Exception e) {
            return PermLevel.MEMBER;
        }
    }

    public static PermLevel getPermLevel(@Nullable Member member) {
        if (member == null) return PermLevel.MEMBER;
        if (isOwner(member.getUser())) return PermLevel.DEVELOPER;
        if (member.getId().equals(Ustawienia.instance.bot.botId)) return PermLevel.ADMINISTRATOR;
        List<Integer> permLevels = member.getRoles().stream()
                .map(f -> PermLevel.getPermLevel(f.getId())).filter(Objects::nonNull)
                .map(PermLevel::getNumer).sorted().collect(Collectors.toList());
        return PermLevel.getPermLevel(permLevels.get(permLevels.size() - 1));
    }

    public static Color getColor(Member member) {
        return member.getRoles().stream().map(Role::getColor).filter(Objects::nonNull).findAny().orElse(Color.green);
    }

    public static String getName(User u) {
        return u.getName() + "#" + u.getDiscriminator();
    }

    public static String getFullName(JDA jda, String id) {
        User u = jda.retrieveUserById(id).complete();
        if (u == null) return null;
        return getFullName(u);
    }

    public static String getFullName(User u) {
        return u.getAsMention() + " [" + getName(u) + "]";
    }

    public static String getFullNameMc(Member mem) {
        return mem.getAsMention() + " [" + getName(mem.getUser()) + "]" + "(`" + getMcNick(mem) + "`)";
    }

    public static String getLogName(User u) {
        return u.getAsMention() + " " + getName(u) + "[" + u.getId() + "]";
    }

    public static String getLogName(Member member) {
        return getLogName(member.getUser());
    }

    public static String getMcNick(@Nullable Member member, boolean seeNick) {
        if (member == null) return "-";
        String nick = member.getNickname();
        if (nick == null) return seeNick ? getName(member.getUser()) : "-";
        if (nick.contains("[")) nick = nick.split("] ")[1];
        return nick;
    }

    public static String getMcNick(@Nullable Member member) {
        return getMcNick(member, false);
    }

    public static List<DiscordRank> getRanks(Member member) {
        return DiscordRank.getRanks(member);
    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    public static String getPrefix(Member mem) {
        try {
            String nick = mem.getNickname().split("] ")[0];
            return nick + "]";
        } catch (Exception e) {
            return null;
        }

    }

}
