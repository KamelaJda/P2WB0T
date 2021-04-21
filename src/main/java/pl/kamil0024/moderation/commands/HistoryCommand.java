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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.database.config.CaseConfig;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.DynamicEmbedPageinator;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.core.util.kary.KaryEnum;
import pl.kamil0024.moderation.listeners.ModLog;

import java.util.*;
import java.util.concurrent.FutureTask;

public class HistoryCommand extends Command {

    private final CaseDao caseDao;
    private final EventWaiter eventWaiter;

    public HistoryCommand(CaseDao caseDao, EventWaiter eventWaiter) {
        name = "history";
        permLevel = PermLevel.CHATMOD;
        category = CommandCategory.MODERATION;
        this.caseDao = caseDao;
        this.eventWaiter = eventWaiter;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        User u = context.getParsed().getUser(context.getArgs().get(0));
        if (u == null) {
            context.send("Nie ma takiego użytkownika!").queue();
            return false;
        }

        Message msg = context.sendTranslate("generic.loading").complete();

        new Thread(() -> {
            try {
                List<CaseConfig> cc = caseDao.getAllDesc(u.getId(), null);

                Map<KaryEnum, Integer> liczbaKar = new HashMap<>();
                for (CaseConfig k : cc) {
                    liczbaKar.put(k.getKara().getTypKary(), liczbaKar.getOrDefault(k.getKara().getTypKary(), 0) + 1);
                }

                List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();

                EmbedBuilder eb = new EmbedBuilder();
                eb.setThumbnail(u.getAvatarUrl());

                eb.setDescription(context.getTranslate("history.history", UserUtil.getLogName(u)));
                eb.setColor(UserUtil.getColor(context.getMember()));

                for (KaryEnum v : KaryEnum.values()) {
                    eb.addField(context.getTranslate("history." + v.toString().toLowerCase()), liczbaKar.getOrDefault(v, 0) + "", true);
                }
                pages.add(new FutureTask<>(() -> eb));

                List<EmbedBuilder> historiaKar = new ArrayList<>();
                for (CaseConfig kara : cc) {
                    EmbedBuilder ebb = ModLog.getEmbed(kara.getKara(), context.getShardManager());
                    boolean aktywna = kara.getKara().getAktywna() != null && kara.getKara().getAktywna();
                    ebb.addField(context.getTranslate("history.aktywna"), aktywna ? context.getTranslate("generic.yes") : context.getTranslate("generic.no"), false);
                    historiaKar.add(ebb);
                }

                Collections.reverse(historiaKar);
                for (EmbedBuilder embedBuilder : historiaKar) {
                    pages.add(new FutureTask<>(() -> embedBuilder));
                }
                new DynamicEmbedPageinator(pages, context.getUser(), eventWaiter, context.getJDA(), 360).create(msg);
            } catch (Exception e) {
                msg.editMessage("Wystąpił błąd: " + e.getMessage()).complete();
                Log.newError(e, getClass());
            }
        }).start();
        return true;
    }

}
