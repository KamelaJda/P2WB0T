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
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.MultiDao;
import pl.kamil0024.core.database.config.MultiConfig;
import pl.kamil0024.core.util.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MultiCommand extends Command {

    private final static SimpleDateFormat SDF = new SimpleDateFormat("dd.MM.yyyy @ HH:mm:ss");

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

        List<MultiConfig> mc;
        String gracz = context.getArgs().get(0);
        boolean showUser = false;
        User user = context.getParsed().getUser(context.getArgs().get(0));
        if (user != null) {
            mc = Collections.singletonList(multiDao.get(user.getId()));
            gracz = UserUtil.getLogName(user);
        } else {
            mc = multiDao.getByNick(context.getArgs().get(0));
            showUser = true;
        }

        if (mc.isEmpty() || mc.get(0).getNicki().isEmpty()) {
            context.send("Taki użytkownik Discorda oraz taki nick w Minecraft nie posiadają żadnych wspólnych kont na Discordzie!").queue();
            return false;
        }

        List<EmbedBuilder> pages = getPages(mc, gracz, context.getMember(), showUser, context.getJDA());

        new EmbedPageintaor(pages, context.getUser(), eventWaiter, context.getJDA()).create(context.getChannel(), context.getMessage());
        msg.delete().queue();
        return true;
    }

    public static String format(Nick nick, @Nullable String user) {
        String format = String.format("`%s - %s %s`", SDF.format(new Date(nick.getDate())), nick.getRanga(), nick.getNick());
        if (user != null) format += " " + user;
        return format;
    }

    public ArrayList<EmbedBuilder> getPages(List<MultiConfig> mc, String name, Member executor, boolean showUser, JDA jda) {
        ArrayList<EmbedBuilder> pages = new ArrayList<>();
        EmbedBuilder eb = new EmbedBuilder();
        eb.addField("Multi konta gracza", name, false);
        eb.setColor(UserUtil.getColor(executor));

        BetterStringBuilder sb = new BetterStringBuilder();

        int index = 1;
        for (MultiConfig multiConfig : mc) {
            for (Nick nick : multiConfig.getNicki()) {
                sb.append(index + ". ");
                sb.appendLine(format(nick, showUser ? UserUtil.getFullName(jda, multiConfig.getId()) : null));
                if (sb.toString().length() >= 1900) {
                    eb.setDescription(sb.toString());
                    pages.add(eb);

                    eb = new EmbedBuilder();
                    eb.setColor(UserUtil.getColor(executor));

                    sb = new BetterStringBuilder();
                }
                index++;
            }
        }

        if (pages.isEmpty()) {
            eb.setDescription(sb.toString());
            pages.add(eb);
        }
        return pages;
    }

}
