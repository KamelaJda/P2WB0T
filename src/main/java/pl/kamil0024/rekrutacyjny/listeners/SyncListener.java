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

package pl.kamil0024.rekrutacyjny.listeners;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.DiscordRank;
import pl.kamil0024.core.util.UserUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("ConstantConditions")
public class SyncListener extends ListenerAdapter {
    
    public SyncListener() { }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (!event.getGuild().getId().equals(Ustawienia.instance.rekrutacyjny.guildId)) return;
        updateMember(event.getMember(), getDerpMember(event.getMember()));
    } 
    
    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        if (!event.getGuild().getId().equals(Ustawienia.instance.bot.guildId)) return;
        updateMember(getRekruMember(event.getMember()), event.getMember());
    }
    
    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        if (!event.getGuild().getId().equals(Ustawienia.instance.bot.guildId)) return;
        Member rekru = getRekruMember(event.getMember());
        List<Role> roles = new ArrayList<>();
        if (rekru == null) return;
        for (Role role : event.getRoles()) {
            String r = role.getId();
            if (r.equals(Ustawienia.instance.roles.adminRole)) roles.add(rekru.getGuild().getRoleById(Ustawienia.instance.rekrutacyjny.admin));
            else if (r.equals(Ustawienia.instance.roles.moderatorRole)) roles.add(rekru.getGuild().getRoleById(Ustawienia.instance.rekrutacyjny.mod));
            else if (r.equals(Ustawienia.instance.roles.helperRole)) roles.add(rekru.getGuild().getRoleById(Ustawienia.instance.rekrutacyjny.pom));
            else if (r.equals(Ustawienia.instance.rangi.stazysta)) roles.add(rekru.getGuild().getRoleById(Ustawienia.instance.rekrutacyjny.staz));
            else if (r.equals(Ustawienia.instance.roles.chatMod)) roles.add(rekru.getGuild().getRoleById(Ustawienia.instance.rekrutacyjny.chatmod));
            else if (r.equals(Ustawienia.instance.rangi.ekipa)) roles.add(rekru.getGuild().getRoleById(Ustawienia.instance.rekrutacyjny.ekipa));
        }
        roles.removeIf(r -> r.equals(null));
        roles.forEach(r -> rekru.getGuild().removeRoleFromMember(rekru, r).queue());
    }

    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        if (!event.getGuild().getId().equals(Ustawienia.instance.bot.guildId)) return;
        updateMember(getRekruMember(event.getMember()), event.getMember());
    }
    
    public static void updateMember(@Nullable Member mem, @Nullable Member derp) {
        if (mem == null || derp == null || UserUtil.getPermLevel(derp).getNumer() == PermLevel.MEMBER.getNumer()) return;

        Ustawienia.Rekrutacyjny ust = Ustawienia.instance.rekrutacyjny;
        String nickname = derp.getNickname();
        List<Role> rolesToAdd = new ArrayList<>();
        List<DiscordRank> ranks = UserUtil.getRanks(derp.getGuild().retrieveMemberById(derp.getId()).complete()); // żeby zaktualizować role

        for (DiscordRank rank : ranks) {
            switch (rank) {
                case ADMINISTRATOR:
                    rolesToAdd.add(mem.getGuild().getRoleById(ust.admin));
                    break;
                case MODERATOR:
                    rolesToAdd.add(mem.getGuild().getRoleById(ust.mod));
                    break;
                case POMOCNIK:
                    rolesToAdd.add(mem.getGuild().getRoleById(ust.pom));
                    break;
                case STAZYSTA:
                    rolesToAdd.add(mem.getGuild().getRoleById(ust.staz));
                    break;
                case EKIPA:
                    List<String> roles = derp.getRoles().stream().map(Role::getId).collect(Collectors.toList());
                    if (!roles.contains(Ustawienia.instance.rangi.stazysta)) {
                        rolesToAdd.add(mem.getGuild().getRoleById(ust.ekipa));
                    }
                    break;
                case CHATMOD:
                    rolesToAdd.add(mem.getGuild().getRoleById(ust.chatmod));
            }
        }

        if (rolesToAdd.isEmpty()) return;

        try {
            if (nickname != null) mem.getGuild().modifyNickname(mem, nickname.replaceAll("STAŻ", "REKRUT")).complete();
            rolesToAdd.forEach(r -> mem.getGuild().addRoleToMember(mem, r).complete());
        } catch (HierarchyException ignored) {
        } catch (Exception e) {
            Log.newError(e, SyncListener.class);
        }
        
    }

    @Nullable
    private Member getDerpMember(Member mem) {
        try {
            return mem.getJDA().getGuildById(Ustawienia.instance.bot.guildId).retrieveMemberById(mem.getId()).complete();
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private Member getRekruMember(Member mem) {
        try {
            return mem.getJDA().getGuildById(Ustawienia.instance.rekrutacyjny.guildId).retrieveMemberById(mem.getId()).complete();
        } catch (Exception e) {
            return null;
        }
    }

}
