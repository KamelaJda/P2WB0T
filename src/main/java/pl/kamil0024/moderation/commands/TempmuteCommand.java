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
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.util.Duration;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.core.util.kary.Kara;
import pl.kamil0024.core.util.kary.KaryEnum;
import pl.kamil0024.moderation.listeners.ModLog;
import pl.kamil0024.stats.StatsModule;

import java.util.Date;
import java.util.Objects;

import static pl.kamil0024.core.util.kary.Kara.check;

public class TempmuteCommand extends Command {

    private final CaseDao caseDao;
    private final ModLog modLog;
    private final StatsModule statsModule;

    public TempmuteCommand(CaseDao caseDao, ModLog modLog, StatsModule statsModule) {
        name = "tempmute";
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

        String[] basic = TempbanCommand.getBasic(context);
        if (basic == null) return false;

        String check = check(context, mem.getUser());
        if (check != null) {
            context.send(check).queue();
            return false;
        }
        String error = tempmute(mem, context.getUser(), basic[1], basic[0], caseDao, modLog, false);
        if (error != null) {
            context.send("Wysąpił błąd! " + error).queue();
            return false;
        }
        context.sendTranslate("tempmute.succes", UserUtil.getLogName(mem), basic[1], basic[0]).queue();
        statsModule.getStatsCache().addZmutowanych(context.getUser().getId(), 1);
        return true;
    }

    public static String tempmute(Member user, User adm, String powod, String duration, CaseDao caseDao, ModLog modLog, boolean isPun) {
        if (UserUtil.getPermLevel(adm).getNumer() <= UserUtil.getPermLevel(user).getNumer()) {
            return "Poziom uprawnień osoby, którą chcesz ukarać jest wyższy od Twojego!";
        }
        Long dur = new Duration().parseLong(duration);
        if (dur == null) {
            return "Duration " + duration + " jest zły!";
        }

        if (MuteCommand.hasMute(user)) {
            return "Ta osoba jest już wyciszona!";
        }

        Role r = user.getGuild().getRoleById(Ustawienia.instance.muteRole);

        try {
            user.getGuild().addRoleToMember(user, Objects.requireNonNull(r)).complete();
        } catch (Exception e) {
            e.printStackTrace();
            return "Nie udalo sie dac muta";
        }
        try {
            user.getGuild().kickVoiceMember(user).complete();
        } catch (Exception ignored) { }

        if (!isPun) {
            Kara kara = new Kara();
            kara.setKaranyId(user.getId());
            kara.setMcNick(UserUtil.getMcNick(user));
            kara.setAdmId(adm.getId());
            kara.setPowod(powod);
            kara.setTimestamp(new Date().getTime());
            kara.setTypKary(KaryEnum.TEMPMUTE);
            kara.setEnd(dur);
            kara.setDuration(duration);
            Kara.put(caseDao, kara, modLog);
        }

        return null;
    }

}
