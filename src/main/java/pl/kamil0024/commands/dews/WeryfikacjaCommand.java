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

import net.dv8tion.jda.api.entities.*;
import pl.kamil0024.api.APIModule;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.config.DiscordInviteConfig;
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
        String arg = context.getArgs().get(0);
        if (arg == null || arg.isEmpty()) throw new UsageException();

        if (arg.equalsIgnoreCase("kod")) {
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

        if (arg.equalsIgnoreCase("manual")) {
            try {
                Member member = context.getParsed().getMember(context.getArgs().get(1));
                if (member == null) {
                    context.send("Nie ma takiego użytkownika!").queue();
                    return false;
                }
                context.getGuild().addRoleToMember(member, context.getGuild().getRoleById(context.getArgs().get(3))).complete();
                context.getGuild().modifyNickname(member, context.getArgs().get(2)).complete();
                context.send("Pomyślnie zweryfikowano!").complete();
            } catch (Exception e) {
                context.send("O nie, wystąpił błąd: " + e.getLocalizedMessage());
            }
        }

        if (arg.equalsIgnoreCase("bypass")) {
            String user = context.getArgs().get(1);
            if (user == null) throw new UsageException();
            weryfikacjaModule.weryfikacjaDao.bypass(user);
            context.send("Użytkownik <@" + user + "> może już wejśc!");
            return true;
        }

        throw new UsageException();
    }

}
