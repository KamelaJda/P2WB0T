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
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.moderation.listeners.ModLog;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.core.util.kary.Kara;
import pl.kamil0024.core.util.kary.KaryEnum;
import pl.kamil0024.stats.StatsModule;

import java.util.Date;

@SuppressWarnings("DuplicatedCode")
public class MuteCommand extends Command {

    private final CaseDao caseDao;
    private final ModLog modLog;
    private final StatsModule statsModule;

    public MuteCommand(CaseDao caseDao, ModLog modLog, StatsModule statsModule) {
        name = "mute";
        permLevel = PermLevel.CHATMOD;
        this.caseDao = caseDao;
        this.modLog = modLog;
        this.statsModule = statsModule;
        category = CommandCategory.MODERATION;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Member mem = context.getParsed().getMember(context.getArgs().get(0));
        if (mem == null) {
            context.sendTranslate("kick.badmember").queue();
            return false;
        }
        Role role = context.getGuild().getRoleById(Ustawienia.instance.muteRole);
        if (role == null) throw new NullPointerException("Rola muteRole jest nullem");
        String powod = context.getArgsToString(1);
        if (powod == null) powod = context.getTranslate("modlog.none");

        if (hasMute(mem)) {
            context.sendTranslate("mute.hasmute").queue();
            return false;
        }

        try {
            context.getGuild().addRoleToMember(mem, role).complete();
            try {
                mem.getGuild().kickVoiceMember(mem).queue();
            } catch (Exception ignored) {}
            context.sendTranslate("mute.succes", UserUtil.getLogName(mem), powod).queue();
            Kara kara = new Kara();
            kara.setKaranyId(mem.getId());
            kara.setMcNick(UserUtil.getMcNick(mem));
            kara.setAdmId(context.getUser().getId());
            kara.setPowod(powod);
            kara.setTimestamp(new Date().getTime());
            kara.setTypKary(KaryEnum.MUTE);
            Kara.put(caseDao, kara, modLog);
            statsModule.getStatsCache().addZmutowanych(context.getUser().getId(), 1);
            return true;
        } catch (InsufficientPermissionException e) {
            context.send("Nie mam odpowiednich permisji: " + e.getPermission().getName()).queue();
        } catch (HierarchyException e) {
            context.send("Moja najwyższa rola jest niżej od najwyższej roli użytkownika, którego chcesz wyciszyć!").queue();
        }
        return false;
    }

    public static boolean hasMute(Member mem) {
        Role r = mem.getGuild().getRoleById(Ustawienia.instance.muteRole);
        if (r == null) throw new NullPointerException("rola muteRole jest nullem");
        return mem.getGuild().retrieveMemberById(mem.getId()).complete().getRoles().contains(r);
    }

}
