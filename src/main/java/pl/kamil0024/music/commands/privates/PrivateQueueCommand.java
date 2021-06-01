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

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.socket.SocketClient;
import pl.kamil0024.core.socket.SocketManager;
import pl.kamil0024.music.commands.QueueCommand;

import java.awt.*;

import static pl.kamil0024.music.commands.QueueCommand.longToTimespan;

@SuppressWarnings("DuplicatedCode")
public class PrivateQueueCommand extends Command {

    private final SocketManager socketManager;

    public PrivateQueueCommand(SocketManager socketManager) {
        name = "pqueue";
        aliases.add("privatequeue");
        category = CommandCategory.PRIVATE_CHANNEL;
        this.socketManager = socketManager;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (!PrivatePlayCommand.check(context)) return false;

        SocketClient client = socketManager.getClientFromChannel(context.getMember());

        if (client == null) {
            context.sendTranslate("pleave.no.bot").queue();
            return false;
        }

        socketManager.getAction(context.getMember().getId(), context.getChannel().getId(), client.getSocketId())
                .queue();
        return true;
    }

    public static class DecodeTrack {

        private final Track trak;
        private final boolean aktualnieGrana;

        public DecodeTrack(String string, boolean aktualnieGrana) {
            this.trak = new Gson().fromJson(string, Track.class);
            this.aktualnieGrana = aktualnieGrana;
        }

        public DecodeTrack(Track trak, boolean aktualnieGrana) {
            this.trak = trak;
            this.aktualnieGrana = aktualnieGrana;
        }

        public EmbedBuilder create() {
            EmbedBuilder eb = new EmbedBuilder();

            eb.setColor(Color.cyan);
            eb.setImage(QueueCommand.getImageUrl(trak.getIdentifier()));

            eb.addField("Tytuł", String.format("[%s](%s)", trak.getTitle(), QueueCommand.getYtLink(trak.getIdentifier())), false);
            eb.addField("Autor", trak.getAuthor(), false);

            if (!aktualnieGrana) {
                eb.addField("Długość", trak.isStream() ? "To jest stream ;p" : longToTimespan(trak.getLength()), true);
            } else {
                eb.addField("Długość", longToTimespan(trak.getLength()), true);
                eb.addField("Pozostało", longToTimespan(trak.getLength() - trak.getPosition()), false);
            }

            return eb;
        }

    }

    @Data
    @AllArgsConstructor
    public static class Track {

        private final String identifier;
        private final String author;
        private final String title;
        private final boolean stream;
        private final long length;
        private final long position;

    }

}
