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

package pl.kamil0024.music.commands.privates;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.wrapper.spotify.model_objects.specification.Track;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.socket.SocketClient;
import pl.kamil0024.core.socket.SocketManager;
import pl.kamil0024.core.util.Error;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.music.MusicModule;
import pl.kamil0024.music.commands.PlayCommand;
import pl.kamil0024.music.commands.QueueCommand;
import pl.kamil0024.music.utils.SpotifyUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("DuplicatedCode")
public class PrivatePlayCommand extends Command {

    private final SocketManager socketManager;
    private final SpotifyUtil spotifyUtil;
    private final MusicModule musicModule;

    public PrivatePlayCommand(SocketManager socketManager, SpotifyUtil spotifyUtil, MusicModule musicModule) {
        name = "pplay";
        aliases.add("privateplay");
        category = CommandCategory.PRIVATE_CHANNEL;
        this.socketManager = socketManager;
        this.spotifyUtil = spotifyUtil;
        this.musicModule = musicModule;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (!check(context)) return false;

        String link = context.getArgs().get(0);
        if (link == null) {
            Error.usageError(context);
            return false;
        }

        SocketClient client = socketManager.getClientFromChannel(context);
        String spotifyMusic = null;

        if (link.contains("https://open.spotify.com/")) {
            // TOOD: Albumy
            if (spotifyUtil.isTrack(link)) {
                Track track;
                try {
                    track = spotifyUtil.getTrack(link);
                } catch (Exception e) {
                    e.printStackTrace();
                    context.send("Wystąpił błąd podczas pobierania piosenki!").queue();
                    return false;
                }
                if (track != null) {
                    List<AudioTrack> audioTrackList = musicModule.search(track.getArtists()[0].getName() + " " + track.getName());
                    if (audioTrackList.isEmpty()) {
                        context.send("Nie znaleziono dopasowań dla tej piosenki Spotify!").queue();
                        return false;
                    }
                    spotifyMusic = QueueCommand.getYtLink(audioTrackList.get(0));
                } else {
                    context.send("Puszczanie piosenek z albumów Spotify zostanie dodane wkrótce!").queue();
                    return false;
                }
            }

        }

        if (client != null) {
            socketManager.getAction(context.getMember().getId(), context.getChannel().getId(), client.getSocketId())
                    .setSendMessage(true).play(spotifyMusic != null ? spotifyMusic : link);
        } else {
            boolean find = false;
            for (Map.Entry<Integer, SocketClient> entry : socketManager.getClients().entrySet()) {
                Member mem = context.getGuild().getMemberById(entry.getValue().getBotId());
                if (mem == null) continue;
                if (mem.getVoiceState() == null || mem.getVoiceState().getChannel() == null) {
                    find = true;
                    socketManager.getAction(context.getMember().getId(), context.getChannel().getId(), entry.getKey())
                            .setSendMessage(false)
                            .connect(PlayCommand.getVc(context.getMember()).getId())
                            .setSendMessage(true)
                            .play(spotifyMusic != null ? spotifyMusic : link);
                    break;
                }
            }
            if (!find) {
                context.sendTranslate("pplay.to.small.bot").queue();
                return false;
            }

        }

        return true;
    }

    public static boolean check(CommandContext context) {
        if (!PlayCommand.isVoice(context.getMember())) {
            context.sendTranslate("pplay.no.channel").queue();
            return false;
        }
        VoiceChannel vc = PlayCommand.getVc(context.getMember());
        if (vc.getParent() == null || !vc.getParent().getName().toLowerCase().contains("prywatne kanały")) {
            context.sendTranslate("pplay.no.private").queue();
            return false;
        }

        if (!context.getMember().hasPermission(vc, Permission.MANAGE_CHANNEL)) {
            context.sendTranslate("pplay.no.channel.owner").queue();
            return false;
        }

        if (UserUtil.getPermLevel(context.getMember()).getNumer() == PermLevel.MEMBER.getNumer()) {
            if (leave(vc)) {
                context.sendTranslate("pplay.min.members").queue();
                return false;
            }
        }

        return true;
    }

    public static boolean leave(VoiceChannel vc) {
        List<Member> members = vc.getMembers().stream()
                .filter(m -> !m.getUser().isBot())
                .collect(Collectors.toList());

        for (Member member : members) {
            try {
                String nick = member.getNickname();
                if (nick == null) continue;

                if (nick.startsWith("[POM]") || nick.startsWith("[MOD]") || nick.startsWith("[ADM]") || nick.startsWith("[STAŻ]")) {
                    return false;
                }

            } catch (Exception ignored) {}
        }
        return members.size() <= 1;
    }

}
