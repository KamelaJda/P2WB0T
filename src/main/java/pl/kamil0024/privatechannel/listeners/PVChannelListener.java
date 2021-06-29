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

package pl.kamil0024.privatechannel.listeners;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.socket.SocketManager;
import pl.kamil0024.core.util.DiscordRank;
import pl.kamil0024.core.util.UserUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class PVChannelListener extends ListenerAdapter {

    private final String primChannel;

    private final Guild guild;
    private final Category mvp;
    private final Category vip;
    private final Category gracz;

    private final SocketManager socketManager;

    public PVChannelListener(ShardManager api, SocketManager socketManager) {
        this.guild = Objects.requireNonNull(api.getGuildById(Ustawienia.instance.bot.guildId));
        this.primChannel = Ustawienia.instance.pv.primChannel;
        this.mvp = Objects.requireNonNull(guild.getCategoryById(Ustawienia.instance.pv.mvp));
        this.vip = Objects.requireNonNull(guild.getCategoryById(Ustawienia.instance.pv.vip));
        this.gracz = Objects.requireNonNull(guild.getCategoryById(Ustawienia.instance.pv.gracz));
        this.socketManager = socketManager;

        VoiceChannel vc = guild.getVoiceChannelById(primChannel);
        if (vc != null) vc.getMembers().forEach(m -> action(m, vc));

        List<VoiceChannel> channels = new ArrayList<>();
        channels.addAll(Objects.requireNonNull(mvp).getVoiceChannels());
        channels.addAll(vip.getVoiceChannels());
        channels.addAll(gracz.getVoiceChannels());
        channels.forEach(this::delete);
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent e) {
        action(e.getMember(), e.getChannelJoined());
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent e) {
        delete(e.getChannelLeft());
        action(e.getMember(), e.getChannelJoined());
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent e) {
        delete(e.getChannelLeft());
    }

    private void delete(VoiceChannel channelLeft) {
        Category parent = channelLeft.getParent();
        if (parent == null || channelLeft.getMembers().size() != 0) return;
        if (parent.getId().equals(mvp.getId()) || parent.getId().equals(vip.getId()) || parent.getId().equals(gracz.getId())) {
            channelLeft.delete().queue();
        }
    }

    private void action(Member member, VoiceChannel vc) {
        if (!vc.getId().equals(primChannel)) return;

        List<VoiceChannel> channels = member.getGuild().getVoiceChannelsByName(Optional.ofNullable(member.getNickname()).orElse(member.getUser().getName()), false);
        if (channels.size() >= 1) {
            try {
                member.getGuild().moveVoiceMember(member, channels.get(0)).complete();
                return;
            } catch (Exception ignored) { }
        }

        DiscordRank rank = UserUtil.getRanks(member).get(0);
        Category cate;
        VoiceChannel created;

        switch (rank) {
            case YT:
            case MVP:
            case OWNER:
            case CHATMOD:
            case MINIYT:
            case MVPPLUS:
            case SPONSOR:
            case BUILDTEAM:
            case POMOCNIK:
            case MVPPLUSPLUS:
            case EKIPA:
                cate = mvp;
                break;
            case VIPPLUS:
            case VIP:
                cate = vip;
                break;
            default:
                cate = gracz;
        }

        try {
            ChannelAction<VoiceChannel> action = guild.createVoiceChannel(member.getNickname() != null ? member.getNickname() : member.getUser().getName())
                    .setParent(cate)
                    .addRolePermissionOverride(guild.getPublicRole().getIdLong(), Permission.getRaw(Permission.VOICE_CONNECT), 0)
                    .addRolePermissionOverride(Long.parseLong(Ustawienia.instance.rangi.ekipa), Permission.getRaw(Permission.MANAGE_CHANNEL, Permission.VOICE_MOVE_OTHERS), 0)
                    .addMemberPermissionOverride(member.getIdLong(), Permission.getRaw(Permission.MANAGE_CHANNEL), 0)
                    .addMemberPermissionOverride(member.getGuild().getSelfMember().getIdLong(), Permission.getRaw(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK), 0)
                    .addMemberPermissionOverride(426505306269286402L, 0, Permission.getRaw(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK))
                    .addMemberPermissionOverride(guild.getSelfMember().getIdLong(), Permission.getRaw(Permission.VOICE_MUTE_OTHERS), 0);
            for (Long musicbots : socketManager.getClients().values().stream().map(m -> Long.parseLong(m.getBotId())).collect(Collectors.toList())) {
                action = action.addRolePermissionOverride(musicbots, Permission.getRaw(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK), 0);
            }
            created = action.complete();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        try {
            member.getGuild().moveVoiceMember(member, created).complete();
        } catch (Exception e) {
            e.printStackTrace();
            created.delete().queue();
        }

    }

}
