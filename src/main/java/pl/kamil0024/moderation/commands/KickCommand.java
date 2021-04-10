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

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.core.util.kary.Kara;
import pl.kamil0024.core.util.kary.KaryEnum;
import pl.kamil0024.moderation.listeners.ModLog;
import pl.kamil0024.stats.StatsModule;

import java.util.Date;

import static pl.kamil0024.core.util.kary.Kara.check;

@SuppressWarnings("DuplicatedCode")
public class KickCommand extends Command {

    private final CaseDao caseDao;
    private final ModLog modLog;
    private final StatsModule statsModule;

    public KickCommand(CaseDao caseDao, ModLog modLog, StatsModule statsModule) {
        name = "kick";
        aliases.add("wyrzuc");
        permLevel = PermLevel.CHATMOD;
        category = CommandCategory.MODERATION;
        this.caseDao = caseDao;
        this.modLog = modLog;
        this.statsModule = statsModule;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Member mem = context.getParsed().getMember(context.getArgs().get(0));
        if (mem == null) {
            context.sendTranslate("kick.badmember").queue();
            return false;
        }
        String powod = context.getArgsToString(1);
        if (powod == null) powod = context.getTranslate("modlog.none");

        String check = check(context, mem.getUser());
        if (check != null) {
            context.send(check).queue();
            return false;
        }

        Message msg = context.send(context.getTranslate("kick.loading")).complete();
        String nick = UserUtil.getMcNick(mem);
        try {
            context.getGuild().kick(mem, powod).complete();
            statsModule.getStatsCache().addWyrzuconych(context.getUser().getId(), 1);
        } catch (InsufficientPermissionException e) {
            msg.editMessage(context.getTranslate("kick.permex")).queue();
            return false;
        } catch (HierarchyException e) {
            msg.editMessage(context.getTranslate("kick.hierarchyex")).queue();
            return false;
        }

        msg.editMessage(context.getTranslate("kick.succes", UserUtil.getLogName(mem), powod)).queue();

        Kara kara = new Kara();
        kara.setKaranyId(mem.getId());
        kara.setMcNick(nick);
        kara.setAdmId(context.getUser().getId());
        kara.setPowod(powod);
        kara.setTimestamp(new Date().getTime());
        kara.setTypKary(KaryEnum.KICK);
        Kara.put(caseDao, kara, modLog);

        return true;
    }

}
