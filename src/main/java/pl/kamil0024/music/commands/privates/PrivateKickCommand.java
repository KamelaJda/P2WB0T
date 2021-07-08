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

package pl.kamil0024.music.commands.privates;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.SlashContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.music.commands.PlayCommand;

public class PrivateKickCommand extends Command {

    public PrivateKickCommand() {
        name = "pkick";
        category = CommandCategory.MUSIC;
        permLevel = PermLevel.MEMBER;
        hideSlash = true;
        commandData = getData()
                .addOption(OptionType.USER, "user", "Użytkownik, który ma być wyrzucony", true);
    }

    @Override
    public boolean execute(SlashContext context) {
        Member mem = context.getEvent().getOption("user").getAsMember();
        if (mem == null) {
            context.send("Nie ma takiego użytkownika");
            return false;
        }

        VoiceChannel memVc = null;
        VoiceChannel argVc = null;

        try {
            memVc = PlayCommand.getVc(context.getMember());
            argVc = PlayCommand.getVc(mem);
        } catch (Exception ignored) { }

        if (memVc == null) {
            context.send("Nie jesteś na żadnym kanale!");
            return false;
        }

        if (argVc == null) {
            context.send("Tego użytkownika nie ma na żadnym kanale!");
            return false;
        }

        if (!memVc.getId().equals(argVc.getId())) {
            context.send("Tego użytkownika nie ma na Twoim kanale!");
            return false;
        }

        if (!context.getMember().hasPermission(memVc, Permission.MANAGE_CHANNEL)) {
            context.send("Musisz być właścicielem kanału!");
            return false;
        }

        if (UserUtil.getPermLevel(mem).getNumer() >= PermLevel.CHATMOD.getNumer()) {
            context.send("Nie możesz wywalić tego użytkownika!");
            return false;
        }

        context.getGuild().moveVoiceMember(mem, null).queue();
        context.send("Użytkownik " + mem.getAsMention() + " opuścił kanał :)");
        return true;
    }

}
