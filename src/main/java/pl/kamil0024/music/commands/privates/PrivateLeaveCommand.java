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

import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.socket.SocketClient;
import pl.kamil0024.core.socket.SocketManager;

@SuppressWarnings("DuplicatedCode")
public class PrivateLeaveCommand extends Command {

    private final SocketManager socketManager;

    public PrivateLeaveCommand(SocketManager socketManager) {
        name = "pleave";
        aliases.add("privateleave");
        category = CommandCategory.PRIVATE_CHANNEL;
        this.socketManager = socketManager;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (!PrivatePlayCommand.check(context)) return false;

        SocketClient client = socketManager.getClientFromChannel(context);
        if (client == null) {
            context.sendTranslate("pleave.no.bot").queue();
            return false;
        }
        socketManager.getAction(context.getMember().getId(), context.getChannel().getId(), client.getSocketId())
                .disconnect();
        client.setVoiceChannel(null);
        return true;

    }

}
