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
import com.wrapper.spotify.model_objects.specification.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.SlashContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.socket.SocketClient;
import pl.kamil0024.core.socket.SocketManager;
import pl.kamil0024.core.util.Tlumaczenia;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.music.MusicModule;
import pl.kamil0024.music.commands.PlayCommand;
import pl.kamil0024.music.commands.QueueCommand;
import pl.kamil0024.music.utils.SpotifyUtil;

import java.util.*;
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
        hideSlash = false;
        commandData = getData()
                .addOption(OptionType.STRING, "link", "Link do piosenki lub playlisty YouTube'a/Spotify", true);
        this.socketManager = socketManager;
        this.spotifyUtil = spotifyUtil;
        this.musicModule = musicModule;
    }

    @Override
    public boolean execute(SlashContext context) {
        if (!check(context)) return false;

        String link = Objects.requireNonNull(context.getEvent().getOption("link")).getAsString();
        List<String> linki = new ArrayList<>();

        String load = Objects.requireNonNull(context.getJDA().getEmoteById(Ustawienia.instance.emote.load)).getAsMention();
        context.send(load + " Ładuje...");

        if (link.contains("https://open.spotify.com/")) {
            List<String> iteml = new ArrayList<>();

            try {
                if (spotifyUtil.isTrack(link)) {
                    Track track = spotifyUtil.getTrackFromUrl(link);
                    if (track != null) iteml.add(track.getArtists()[0].getName() + " " + track.getName());
                } else if (spotifyUtil.isAlbum(link)) {
                    Album album = spotifyUtil.getAlbumFromUrl(link);
                    for (TrackSimplified item : album.getTracks().getItems()) {
                        iteml.add(item.getArtists()[0].getName() + " " + item.getName());
                        if (iteml.size() == 10) break;
                    }
                } else if (spotifyUtil.isArtists(link)) {
                    Track[] tracks = spotifyUtil.getArtistsTracks(link);
                    for (Track track : tracks) {
                        iteml.add(track.getArtists()[0].getName() + " " + track.getName());
                        if (iteml.size() == 10) break;
                    }
                } else if (spotifyUtil.isPlaylist(link)) {
                    context.getChannel().sendTyping().queue();
                    Paging<PlaylistTrack> album = spotifyUtil.getPlaylistFromUrl(link);
                    List<PlaylistTrack> items = Arrays.stream(album.getItems())
                            .filter(s -> !s.getIsLocal())
                            .collect(Collectors.toList());
                    Collections.reverse(items);

                    for (PlaylistTrack item : items) {
                        try {
                            Track track = (Track) item.getTrack();
                            iteml.add(track.getArtists()[0].getName() + " " + track.getName());
                            if (iteml.size() == 10) break;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (iteml.isEmpty()) {
                        context.send("Nie znaleziono żadnych piosenek w tej playliście!");
                        return false;
                    }
                }

                if (!iteml.isEmpty()) {
                    for (String s : iteml) {
                        List<AudioTrack> audioTrackList = musicModule.search(s);
                        if (!audioTrackList.isEmpty()) {
                            linki.add(QueueCommand.getYtLink(audioTrackList.get(0)));
                        }
                    }
                } else {
                    context.send("Nie znaleziono nic pod tym linkiem! (jeżeli miał on odtworzyć piosenkę(-i), zgłoś to do administracji!)");
                    return false;
                }

            } catch (Exception e) {
                e.printStackTrace();
                context.send("Wystąpił błąd podczas pobierania piosenki!");
                return false;
            }

        }

        SocketClient client = socketManager.getClientFromChannel(context.getMember());

        if (client != null) {
            SocketManager.Action sm = socketManager.getAction(context.getMember().getId(), context.getChannel().getId(), client.getSocketId(), context.getHook())
                    .setSendMessage(true);
            if (!linki.isEmpty()) {
                sm.play(linki);
                return true;
            }
            sm.play(link);
        } else {
            boolean find = false;
            for (Map.Entry<Integer, SocketClient> entry : socketManager.getClients().entrySet()) {
                Member mem = context.getGuild().getMemberById(entry.getValue().getBotId());
                if (mem == null) continue;
                if (mem.getVoiceState() == null || mem.getVoiceState().getChannel() == null) {
                    find = true;
                    SocketManager.Action sm = socketManager.getAction(context.getMember().getId(), context.getChannel().getId(), entry.getKey(), context.getHook())
                            .setSendMessage(false)
                            .connect(PlayCommand.getVc(context.getMember()).getId())
                            .setSendMessage(true);
                    if (!linki.isEmpty()) {
                        sm.play(linki);
                        break;
                    }
                    sm.setSendMessage(true).play(link);
                    break;
                }
            }
            if (!find) {
                context.sendTranslate("pplay.to.small.bot");
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

    public static boolean check(SlashContext context) {
        if (!PlayCommand.isVoice(context.getMember())) {
            context.getHook().sendMessage(Tlumaczenia.get("pplay.no.channel")).queue();
            return false;
        }
        VoiceChannel vc = PlayCommand.getVc(context.getMember());
        if (vc.getParent() == null || !vc.getParent().getName().toLowerCase().contains("prywatne kanały")) {
            context.getHook().sendMessage(Tlumaczenia.get("pplay.no.private")).queue();
            return false;
        }

        if (!context.getMember().hasPermission(vc, Permission.MANAGE_CHANNEL)) {
            context.getHook().sendMessage(Tlumaczenia.get("pplay.no.channel.owner")).queue();
            return false;
        }

        if (UserUtil.getPermLevel(context.getMember()).getNumer() == PermLevel.MEMBER.getNumer()) {
            if (leave(vc)) {
                context.getHook().sendMessage(Tlumaczenia.get("pplay.min.members")).queue();
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

            } catch (Exception ignored) { }
        }
        return members.size() <= 1;
    }

}
