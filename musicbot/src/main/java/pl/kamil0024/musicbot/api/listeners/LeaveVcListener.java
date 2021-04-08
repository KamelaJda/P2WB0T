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

package pl.kamil0024.musicbot.api.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import pl.kamil0024.musicbot.core.Ustawienia;
import pl.kamil0024.musicbot.core.util.EventWaiter;
import pl.kamil0024.musicbot.music.managers.MusicManager;
import pl.kamil0024.musicbot.socket.SocketClient;

import java.util.List;
import java.util.stream.Collectors;

public class LeaveVcListener extends ListenerAdapter {

    private final MusicManager musicManager;
    private final LeaveWaiter leaveWaiter;
    private final SocketClient client;

    public LeaveVcListener(MusicManager musicManager, EventWaiter eventWaiter, SocketClient client) {
        this.musicManager = musicManager;
        this.leaveWaiter = new LeaveWaiter(eventWaiter, musicManager);
        this.client = client;
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if (!event.getGuild().getId().equals(Ustawienia.instance.bot.guildId) ||
                !event.getEntity().getId().equals(event.getGuild().getSelfMember().getId())) {
            return;
        }
        Guild guild = event.getGuild();
        if (!guild.getId().equals(Ustawienia.instance.bot.guildId)) {
            return;
        }
        musicManager.getMusicManagers().get(guild.getIdLong()).destroy();
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (!event.getGuild().getId().equals(Ustawienia.instance.bot.guildId)) return;
        Guild guild = event.getGuild();
        if (!guild.getId().equals(Ustawienia.instance.bot.guildId)) {
            return;
        }
        if (event.getEntity().getId().equals(event.getGuild().getSelfMember().getId())) {
            musicManager.getMusicManagers().get(guild.getIdLong()).destroy();
            return;
        }

        GuildVoiceState state = event.getGuild().getSelfMember().getVoiceState();
        if (state == null || state.getChannel() == null || !state.getChannel().getId().equals(event.getChannelLeft().getId())) {
            return;
        }

        if (leave(event.getChannelLeft())) {
            leaveWaiter.initWaiter(event.getChannelLeft(), client);
        }

    }

    public static boolean leave(VoiceChannel vc) {
        List<Member> members = vc.getMembers().stream()
                .filter(m -> !m.getUser().isBot())
                .collect(Collectors.toList());

        for (Member member : members) {
            try {
                String nick = member.getNickname();
                if (nick == null) continue;

                if (nick.startsWith("[POM]") || nick.startsWith("[MOD]") || nick.startsWith("[ADM]")) {
                    return false;
                }

            } catch (Exception ignored) {}
        }

        return members.size() <= 1;
    }

}
