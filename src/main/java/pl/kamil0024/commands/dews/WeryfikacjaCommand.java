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

package pl.kamil0024.commands.dews;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import pl.kamil0024.api.APIModule;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.SubCommand;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.config.DiscordInviteConfig;
import pl.kamil0024.core.database.config.WeryfikacjaConfig;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.UsageException;
import pl.kamil0024.weryfikacja.WeryfikacjaModule;

public class WeryfikacjaCommand extends Command {

    private final APIModule apiModule;
    private final WeryfikacjaModule weryfikacjaModule;

    public WeryfikacjaCommand(APIModule apiModule, WeryfikacjaModule weryfikacjaModule) {
        name = "weryfikacja";
        aliases.add("wer");
        permLevel = PermLevel.ADMINISTRATOR;
        category = CommandCategory.DEVS;
        this.apiModule = apiModule;
        this.weryfikacjaModule = weryfikacjaModule;
    }

    @Override
    public boolean execute(CommandContext context) {
        throw new UsageException();
    }

    @SubCommand(name = "kod", aliases = {"code"})
    public boolean kod(CommandContext context) {
        String kod = context.getArgs().get(1);
        String user = context.getArgs().get(2);
        if (kod == null || user == null) {
            context.send("Kod lub user jest nullem!").queue();
            return false;
        }
        Member member = context.getParsed().getMember(user);
        if (member == null) {
            context.send("member nullem!").queue();
            return false;
        }

        Message msg = context.send("Próbuje automatycznie zweryfikować...").complete();
        DiscordInviteConfig conf = apiModule.getNewWery().getIfPresent(member.getId());
        if (conf != null) {
            weryfikacjaModule.executeCode(member.getId(), conf, context.getChannel(), context.getGuild(), true);
            apiModule.getNewWery().invalidate(member.getId());
            return true;
        }
        msg.editMessage("Próbuje zweryfikować przez kod...").complete();
        weryfikacjaModule.executeCode(member, context.getChannel(), kod, context.getGuild(), true);
        return true;
    }

    @SubCommand(name = "manual")
    public boolean manual(CommandContext context) {
        try {
            Member member = context.getParsed().getMember(context.getArgs().get(1));
            if (member == null) {
                context.send("Nie ma takiego użytkownika!").queue();
                return false;
            }
            if (context.getArgs().get(2) == null || context.getArgs().get(3) == null) throw new UsageException();

            Role role = context.getGuild().getRoleById(context.getArgs().get(2));
            String nick = context.getArgsToString(3);

            if (role == null) {
                context.send("Nie ma takiej roli!").queue();
                return false;
            }

            context.getGuild().addRoleToMember(member, role).complete();
            context.getGuild().modifyNickname(member, nick).complete();
            context.send("Pomyślnie zweryfikowano!").complete();
            return true;
        } catch (Exception e) {
            context.send("O nie, wystąpił błąd: " + e.getLocalizedMessage());
            Log.newError(e, getClass());
            return false;
        }
    }

    @SubCommand(name = "bypass")
    public boolean bypass(CommandContext context) {
        String user = context.getArgs().get(1);
        if (user == null) throw new UsageException();
        weryfikacjaModule.weryfikacjaDao.bypass(user);
        context.send("Użytkownik <@" + user + "> może już wejść").queue();
        return true;
    }

    @SubCommand(name = "disable")
    public boolean disable(CommandContext context) {
        String nick = context.getArgs().get(1);
        if (nick == null) throw new UsageException();
        WeryfikacjaConfig config = weryfikacjaModule.weryfikacjaDao.get(nick);
        config.setDisabled(true);
        weryfikacjaModule.weryfikacjaDao.save(config);
        context.send("Można już wejść na tym nicku!").queue();
        return true;
    }

}
