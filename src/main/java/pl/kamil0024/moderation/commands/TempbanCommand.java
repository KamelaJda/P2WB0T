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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.util.Duration;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.core.util.kary.Dowod;
import pl.kamil0024.core.util.kary.Kara;
import pl.kamil0024.core.util.kary.KaryEnum;
import pl.kamil0024.moderation.listeners.ModLog;

import java.util.Date;
import java.util.List;

import static pl.kamil0024.core.util.kary.Kara.check;

@SuppressWarnings("DuplicatedCode")
public class TempbanCommand extends Command {

    private final CaseDao caseDao;
    private final ModLog modLog;

    public TempbanCommand(CaseDao caseDao, ModLog modLog) {
        name = "tempban";
        permLevel = PermLevel.CHATMOD;
        category = CommandCategory.MODERATION;
        this.caseDao = caseDao;
        this.modLog = modLog;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Member member = context.getParsed().getMember(context.getArgs().get(0));
        User user = context.getParsed().getUser(context.getArgs().get(0));
        if (user == null) {
            context.sendTranslate("kick.badmember").queue();
            return false;
        }
        String duration = context.getArgs().get(1);
        String powod = context.getArgsToString(2);
        if (duration == null) {
            context.send(context.getTranslate("tempban.badtime")).queue();
            return false;
        }
        if (powod == null) powod = context.getTranslate("modlog.none");

        String check = check(context, user);
        if (check != null) {
            context.send(check).queue();
            return false;
        }
        String error = tempban(user, context.getUser(), powod, duration, caseDao, modLog, false, context.getGuild(), UserUtil.getMcNick(member));
        if (error != null) {
            context.send("Wysąpił błąd! " + error).queue();
            return false;
        }
        context.sendTranslate("tempban.succes", UserUtil.getLogName(user), powod, duration).queue();
        return true;
    }

    public static String tempban(User user, User adm, String powod, String duration, CaseDao caseDao, ModLog modLog, boolean isPun, Guild guild, String nick) {
        return tempban(user, adm, powod, duration, caseDao, modLog, isPun, guild, nick, 0, null);
    }

    public static String tempban(User user, User adm, String powod, String duration, CaseDao caseDao, ModLog modLog, boolean isPun, Guild guild, String nick, int delDays, List<Dowod> dowody) {
        if (UserUtil.getPermLevel(adm).getNumer() <= UserUtil.getPermLevel(user).getNumer()) {
            return "Poziom uprawnień osoby, którą chcesz ukarać jest wyższy od Twojego!";
        }
        Long dur = new Duration().parseLong(duration);
        if (dur == null) {
            return "Duration `" + duration + "` jest zły!";
        }

        for (Guild.Ban ban : guild.retrieveBanList().complete()) {
            if (ban.getUser().getId().equals(user.getId())) {
                return "Ta osoba jest już zbanowana!";
            }
        }

        if (!isPun) {
            Kara kara = new Kara();
            kara.setKaranyId(user.getId());
            kara.setMcNick(nick);
            kara.setAdmId(adm.getId());
            kara.setPowod(powod);
            kara.setTimestamp(new Date().getTime());
            kara.setTypKary(KaryEnum.TEMPBAN);
            kara.setEnd(dur);
            kara.setDuration(duration);
            if (dowody != null) kara.setDowody(dowody);
            Kara.put(caseDao, kara, modLog);
        }

        guild.ban(user, delDays, powod).complete();
        return null;
    }

}
