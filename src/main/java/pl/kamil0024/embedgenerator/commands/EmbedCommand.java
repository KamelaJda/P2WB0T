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

package pl.kamil0024.embedgenerator.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.util.UsageException;
import pl.kamil0024.embedgenerator.entity.EmbedRedisManager;

public class EmbedCommand extends Command {

    private final EmbedRedisManager embedRedisManager;

    public EmbedCommand(EmbedRedisManager embedRedisManager) {
        name = "embed";
        aliases.add("embeds");
        permLevel = PermLevel.ADMINISTRATOR;
        this.embedRedisManager = embedRedisManager;
    }

    @Override
    public boolean execute(CommandContext context) {
        String firsta = context.getArgs().get(0);
        if (firsta == null || (!firsta.equalsIgnoreCase("send")) && !firsta.equalsIgnoreCase("edit")) throw new UsageException();

        TextChannel kanal = context.getParsed().getTextChannel(context.getArgs().get(0));
        if (kanal == null) {
            context.send("Musisz podać kanał!").queue();
            return false;
        }
        if (!kanal.canTalk() || context.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS)) {
            context.send("Nie mam odpowiednich permisji!").queue();
            return false;
        }
        String kod = context.getArgs().get(1);
        if (kod == null) {
            context.send("Musiz podać kod embeda!").queue();
            return false;
        }
        EmbedBuilder eb = embedRedisManager.get(kod);
        if (eb == null) {
            context.send("Kod jest nieprawidłowy!").queue();
            return false;
        }
        if (firsta.equalsIgnoreCase("send")) {
            kanal.sendMessage(eb.build()).complete();
            context.send("Pomyślnie wysłano!").queue();
            return true;
        }
        if (firsta.equalsIgnoreCase("edit")) {
            Message msg;
            try {
                msg = kanal.retrieveMessageById(context.getArgs().get(2)).complete();
            } catch (Exception e) {
                context.send("Nie udało się uzyskać wiadomości!").queue();
                return false;
            }
            msg.editMessage(eb.build()).queue();
            context.send("Pomyślnie edytowano!").queue();
            return true;
        }
        throw new UsageException();
    }

}
