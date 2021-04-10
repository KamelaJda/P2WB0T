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


import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.RemindDao;
import pl.kamil0024.core.database.config.RemindConfig;
import pl.kamil0024.core.util.*;
import pl.kamil0024.moderation.listeners.ModLog;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.FutureTask;

public class RemindmeCommand extends Command {

    private final RemindDao remindDao;
    private final EventWaiter eventWaiter;

    public RemindmeCommand(RemindDao remindDao, EventWaiter eventWaiter) {
        name = "remind";
        aliases.add("remindme");
        permLevel = PermLevel.HELPER;
        this.remindDao = remindDao;
        this.eventWaiter = eventWaiter;
        enabledInRekru = true;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String arg = context.getArgs().get(0);
        if (arg == null) throw new UsageException();

        if (arg.equalsIgnoreCase("list")) {
            List<RemindConfig> rc = remindDao.getAll();
            rc.removeIf(m -> !m.getUserId().equals(context.getUser().getId()));
            if (rc.isEmpty()) {
                context.send(context.getTranslate("remind.remindlist")).queue();
                return false;
            }
            ArrayList<FutureTask<EmbedBuilder>> pages = new ArrayList<>();

            for (RemindConfig conf : rc) {
                pages.add(new FutureTask<>(() -> getEmbed(conf).setColor(UserUtil.getColor(context.getMember()))));
            }
            new DynamicEmbedPageinator(pages, context.getUser(), eventWaiter, context.getJDA(), 120).create(context.getChannel(), context.getMessage());
            return true;
        }

        if (arg.equalsIgnoreCase("delete") || arg.equalsIgnoreCase("remove")) {
            Integer id = context.getParsed().getNumber(context.getArgs().get(1));
            if (id == null) throw new UsageException();

            List<RemindConfig> rc = remindDao.getAll();
            rc.removeIf(m -> !m.getUserId().equals(context.getUser().getId()));
            if (rc.isEmpty()) {
                context.send(context.getTranslate("remind.remindlist")).queue();
                return false;
            }
            RemindConfig ids = rc.stream().filter(conf -> conf.getId().equals(id.toString())).findAny().orElse(null);
            if (ids == null) {
                context.sendTranslate("remind.badid").queue();
                return false;
            }
            remindDao.remove(ids);
            context.sendTranslate("remind.successdelete").queue();
            return true;
        }

        Long dur = new Duration().parseLong(arg);
        if (dur == null) {
            context.send(context.getTranslate("cytuj.badtime")).queue();
            return false;
        }

        String tresc = context.getArgsToString(1);
        if (tresc == null) {
            context.send(context.getTranslate("remind.blank")).queue();
            return false;
        }
        RemindConfig rc = remindDao.get(remindDao.getNextId());
        rc.setUserId(context.getUser().getId());
        rc.setCzas(dur);
        rc.setTresc(tresc);
        rc.setMsg(context.getMessage().getJumpUrl());
        remindDao.save(rc);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy `@` HH:mm:ss");
        context.send(context.getTranslate("remind.save", sdf.format(new Date(dur)))).queue();
        return true;
    }

    private EmbedBuilder getEmbed(RemindConfig remind) {
        long dzisiaj = new Date().getTime();
        BDate bd = new BDate(dzisiaj, ModLog.getLang());
        EmbedBuilder eb = new EmbedBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        eb.addField("Treść przypomnienia", remind.getTresc(), false);
        eb.addField("Wiadomość przypomnienia", String.format("[KLIK](%s)", remind.getMsg()), false);
        eb.addField("Zaplanowano na", bd.difference(remind.getCzas()), false);
        eb.addField("Dzień zaplanowania", sdf.format(new Date(remind.getCzas())), false);
        eb.setColor(Color.cyan);
        return eb;
    }

    public static void check(RemindDao remindDao, ShardManager api) {
        long teraz = new Date().getTime();
        for (RemindConfig remind : remindDao.getAll()) {
            if (remind.getCzas() - teraz <= 0) {
                BetterStringBuilder sb = new BetterStringBuilder();
                sb.appendLine("⏰ **Przypomnienie**:" + remind.getTresc());
                sb.appendLine("Przypomnienie ustawione w tej wiadomości:");
                sb.append(remind.getMsg());

                try {
                    User u = api.retrieveUserById(remind.getUserId()).complete();
                    u.openPrivateChannel().complete().sendMessage(sb.build()).queue();
                } catch (Exception ignored) {}

                remindDao.remove(remind);
            }
        }

    }

}
