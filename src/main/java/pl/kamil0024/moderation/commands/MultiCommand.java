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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.MultiDao;
import pl.kamil0024.core.database.config.MultiConfig;
import pl.kamil0024.core.util.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MultiCommand extends Command {

    private final MultiDao multiDao;
    private final EventWaiter eventWaiter;

    public MultiCommand(MultiDao multiDao, EventWaiter eventWaiter) {
        name = "multi";
        permLevel = PermLevel.CHATMOD;
        category = CommandCategory.MODERATION;
        this.multiDao = multiDao;
        this.eventWaiter = eventWaiter;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Message msg = context.send("Ładuje...").reference(context.getMessage()).complete();

        MultiConfig mc;

        User user = context.getParsed().getUser(context.getArgs().get(0));
        if (user != null) mc = multiDao.get(user.getId());
        else mc = multiDao.getByNick(context.getArgs().get(0));

        if (mc.getNicki().isEmpty()) {
            context.send("Taki użytkownik Discorda oraz taki nick w Minecraft nie posiadają żadnych wspólnych kont na Discordzie!").queue();
            return false;
        }

        List<EmbedBuilder> pages = getPages(mc, "", context.getMember());

        new EmbedPageintaor(pages, context.getUser(), eventWaiter, context.getJDA()).create(context.getChannel(), context.getMessage());
        msg.delete().queue();
        return true;
    }

    public static String format(Nick nick) {
        SimpleDateFormat sfd = new SimpleDateFormat("dd.MM.yyyy `@` HH:mm:ss");
        return sfd.format(new Date(nick.getDate())) + " - " + nick.getRanga() + " " + nick.getNick();
    }

    public ArrayList<EmbedBuilder> getPages(MultiConfig mc, String name, Member executor) {
        ArrayList<EmbedBuilder> pages = new ArrayList<>();
        EmbedBuilder eb = new EmbedBuilder();
        eb.addField("Multi konta gracza", name, false);
        eb.setColor(UserUtil.getColor(executor));

        BetterStringBuilder sb = new BetterStringBuilder();
        sb.appendLine("```");

        for (Nick nick : mc.getNicki()) {
            sb.appendLine(format(nick));
            if (sb.toString().length() >= 1900) {
                sb.append("```");
                eb.setDescription(sb.toString());
                pages.add(eb);

                eb = new EmbedBuilder();
                eb.setColor(UserUtil.getColor(executor));

                sb = new BetterStringBuilder();
                sb.appendLine("```");
            }
        }

        if (pages.isEmpty()) {
            sb.append("```");
            eb.setDescription(sb.toString());
            pages.add(eb);
        }
        return pages;
    }

}
