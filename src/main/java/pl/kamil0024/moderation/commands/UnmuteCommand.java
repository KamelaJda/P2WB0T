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
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.Ustawienia;
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
import java.util.Objects;

public class UnmuteCommand extends Command {

    private final CaseDao caseDao;
    private final ModLog modLog;

    public UnmuteCommand(CaseDao caseDao, ModLog modLog) {
        name = "unmute";
        permLevel = PermLevel.CHATMOD;
        category = CommandCategory.MODERATION;
        this.caseDao = caseDao;
        this.modLog = modLog;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Member mem = context.getParsed().getMember(context.getArgs().get(0));
        if (mem == null) {
            context.sendTranslate("kick.badmember").queue();
            return false;
        }

        if (!MuteCommand.hasMute(mem)) {
            context.send("Taki użytkownik nie jest wyciszony!");
            return false;
        }
        Role r = mem.getGuild().getRoleById(Ustawienia.instance.muteRole);
        String powod = context.getArgsToString(1);
        if (powod == null) {
            context.send(context.getTranslate("unban.reason")).queue();
            return false;
        }
        try {
            context.getGuild().removeRoleFromMember(mem, Objects.requireNonNull(r)).complete();
            context.sendTranslate("unmute.succes", UserUtil.getName(mem.getUser()), powod).queue();

            for (CaseConfig kara1 : caseDao.getAktywe(mem.getId())) {
                if (kara1.getKara().getTypKary() == KaryEnum.MUTE || kara1.getKara().getTypKary() == KaryEnum.TEMPMUTE) {
                    caseDao.delete(kara1.getKara().getKaraId());
                    kara1.getKara().setAktywna(false);
                    caseDao.save(kara1);
                }
            }

            Kara kara = new Kara();
            kara.setKaranyId(mem.getId());
            kara.setMcNick(UserUtil.getMcNick(mem));
            kara.setAdmId(context.getUser().getId());
            kara.setPowod(powod);
            kara.setTimestamp(new Date().getTime());
            kara.setTypKary(KaryEnum.UNMUTE);
            Kara.put(caseDao, kara, modLog);
            return true;
        } catch (Exception e) {
            context.send("Nie udało się odcyszyć użytkownika! " + e.getMessage());
            return false;
        }
    }

}
