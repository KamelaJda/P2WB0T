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
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.util.DiscordRank;
import pl.kamil0024.core.util.UserUtil;

import java.util.Objects;

public class PVChannelListener extends ListenerAdapter {

    private final String primChannel;

    private final Category mvp;
    private final Category vip;
    private final Category gracz;

    public PVChannelListener(ShardManager api) {
        Guild guild = Objects.requireNonNull(api.getGuildById(Ustawienia.instance.bot.guildId));
        // TODO: Ustawienia
        this.primChannel = guild.getVoiceChannelById("533651508710080533").getId();
        this.mvp = guild.getCategoryById("535436156947398666");
        this.vip = guild.getCategoryById("535442075274182666");
        this.gracz = guild.getCategoryById("535433287657717770");
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
        if (parent == null) return;
        if (parent.getId().equals(mvp.getId()) || parent.getId().equals(vip.getId()) || parent.getId().equals(gracz.getId())) {
            channelLeft.delete().queue();
        }
    }

    private void action(Member member, VoiceChannel vc) {
        if (!vc.getId().equals(primChannel)) return;
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
            created = cate.createVoiceChannel(member.getNickname() != null ? member.getNickname() : member.getUser().getName())
                    .addMemberPermissionOverride(member.getIdLong(), Permission.getRaw(Permission.MANAGE_CHANNEL), 0)
//                    .addMemberPermissionOverride(member.getIdLong(), Permission.getRaw(Permission.VOICE_MOVE_OTHERS), 0)
                    .complete();
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
