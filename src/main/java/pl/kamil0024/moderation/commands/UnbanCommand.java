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

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.database.config.CaseConfig;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.core.util.kary.Kara;
import pl.kamil0024.core.util.kary.KaryEnum;
import pl.kamil0024.moderation.listeners.ModLog;

import java.util.Date;
import java.util.List;

@SuppressWarnings("DuplicatedCode")
public class UnbanCommand extends Command {

    private final CaseDao caseDao;
    private final ModLog modLog;

    public UnbanCommand(CaseDao caseDao, ModLog modLog) {
        name = "unban";
        permLevel = PermLevel.CHATMOD;
        category = CommandCategory.MODERATION;
        this.caseDao = caseDao;
        this.modLog = modLog;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        User u = context.getParsed().getUser(context.getArgs().get(0));
        if (u == null) {
            context.sendTranslate("kick.badmember");
            return false;
        }

        String powod = context.getArgsToString(1);
        if (powod == null) {
            context.send(context.getTranslate("unban.reason")).queue();
            return false;
        }

        try {
            boolean jest = false;
            List<Guild.Ban> bans = context.getGuild().retrieveBanList().complete();
            for (Guild.Ban b : bans) {
                if (b.getUser().getId().equals(u.getId())) {
                    jest = true;
                    break;
                }
            }
            if (!jest) {
                context.sendTranslate("unban.donthave").queue();
                return false;
            }

            context.getGuild().unban(u).complete();
            context.sendTranslate("unban.succes", UserUtil.getName(u), powod).queue();

            for (CaseConfig kara1 : caseDao.getAktywe(u.getId())) {
                if (kara1.getKara().getTypKary() == KaryEnum.TEMPBAN || kara1.getKara().getTypKary() == KaryEnum.BAN) {
                    caseDao.delete(kara1.getKara().getKaraId());
                    kara1.getKara().setAktywna(false);
                    caseDao.save(kara1);
                }
            }

            Kara kara = new Kara();
            kara.setKaranyId(u.getId());
            kara.setMcNick("-");
            kara.setAdmId(context.getUser().getId());
            kara.setPowod(powod);
            kara.setTimestamp(new Date().getTime());
            kara.setTypKary(KaryEnum.UNBAN);
            Kara.put(caseDao, kara, modLog);
            return true;
        } catch (Exception e) {
            context.send("Wystąpił błąd przy zdejmowaniu unbana: " + e.getLocalizedMessage()).queue();
            return false;
        }
    }

}
