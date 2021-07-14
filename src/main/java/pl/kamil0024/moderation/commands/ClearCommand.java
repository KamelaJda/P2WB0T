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

package pl.kamil0024.moderation.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.stats.StatsModule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.DAYS;

public class ClearCommand extends Command {

    private final StatsModule statsModule;

    public ClearCommand(StatsModule statsModule) {
        name = "clear";
        aliases.add("purge");
        permLevel = PermLevel.CHATMOD;
        category = CommandCategory.MODERATION;

        this.statsModule = statsModule;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        User user = null;
        Integer liczba;
        TextChannel kanal;

        if (context.getParsed().getUser(context.getArgs().get(0)) == null) {
            liczba = context.getParsed().getNumber(context.getArgs().get(0));
            kanal = context.getParsed().getTextChannel(context.getArgs().get(1));

        } else {
            user = context.getParsed().getUser(context.getArgs().get(0));
            liczba = context.getParsed().getNumber(context.getArgs().get(1));
            kanal = context.getParsed().getTextChannel(context.getArgs().get(2));
        }
        if (kanal == null) kanal = context.getChannel();

        if (liczba == null || liczba > 100 || liczba <= 1) {
            context.send(context.getTranslate("clear.toolong")).queue();
            return true;
        }
        CompletableFuture<MessageHistory> historia;
        if (user == null) historia = kanal.getHistoryBefore(context.getMessage(), liczba).submit();
        else historia = kanal.getHistoryBefore(context.getMessage(), 100).submit();

        long dwaTygodnieTemu = (System.currentTimeMillis() - DAYS.toMillis(14) - TimeUtil.DISCORD_EPOCH) << TimeUtil.TIMESTAMP_OFFSET;
        List<Message> wiadomosciWszystkie = new ArrayList<>();
        List<Message> wiadomosci = historia.join().getRetrievedHistory();

        context.getMessage().delete().queue();
        for (Message msg : wiadomosci) {
            if (wiadomosciWszystkie.size() != liczba && msg.getIdLong() > dwaTygodnieTemu) {
                if (user != null && msg.getAuthor().getId().equals(user.getId())) wiadomosciWszystkie.add(msg);
                else if (user == null) wiadomosciWszystkie.add(msg);
            }
        }
        statsModule.getStatsCache().addUsunietychWiadomosci(context.getUser().getId(), wiadomosciWszystkie.size());
        kanal.purgeMessages(wiadomosciWszystkie);
        return true;
    }

}
