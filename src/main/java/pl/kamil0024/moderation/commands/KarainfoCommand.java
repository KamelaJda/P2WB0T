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
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.database.config.CaseConfig;
import pl.kamil0024.core.util.EmbedPaginator;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.core.util.UsageException;
import pl.kamil0024.core.util.kary.Dowod;
import pl.kamil0024.moderation.listeners.ModLog;

import java.util.ArrayList;
import java.util.List;

public class KarainfoCommand extends Command {

    private final CaseDao caseDao;
    private final EventWaiter eventWaiter;

    public KarainfoCommand(CaseDao caseDao, EventWaiter eventWaiter) {
        name = "karainfo";
        aliases.add("infokara");
        permLevel = PermLevel.CHATMOD;
        category = CommandCategory.MODERATION;

        this.caseDao = caseDao;
        this.eventWaiter = eventWaiter;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Integer id = context.getParsed().getNumber(context.getArgs().get(0));
        if (id == null) throw new UsageException();

        CaseConfig cc = caseDao.get(id);
        if (cc.getKara() == null) {
            context.send(context.getTranslate("karainfo.invalid")).queue();
            return false;
        }
        List<EmbedBuilder> list = new ArrayList<>();
        list.add(ModLog.getEmbed(cc.getKara(), context.getShardManager(), false, true));
        if (cc.getKara().getDowody() != null && !cc.getKara().getDowody().isEmpty()) {
            for (Dowod dowod : cc.getKara().getDowody()) {
                list.add(DowodCommand.getEmbed(dowod, context));
            }
        }

        new EmbedPaginator(list, context.getUser(), eventWaiter).create(context.getChannel());
        return true;
    }

}
