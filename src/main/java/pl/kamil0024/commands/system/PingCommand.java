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

package pl.kamil0024.commands.system;

import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.commands.dews.ShellCommand;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;

import java.time.temporal.ChronoUnit;

public class PingCommand extends Command {

    public PingCommand() {
        name = "ping";
        cooldown = 15;
        enabledInRekru = true;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Message msg = context.send(context.getTranslate("ping.ping")).complete();
        long ping = context.getEvent().getMessage().getTimeCreated().until(msg.getTimeCreated(), ChronoUnit.MILLIS);
        msg.editMessage(context.getTranslate("ping.pong", ping, context.getEvent().getJDA().getGatewayPing())).queue();
        return true;
    }

}