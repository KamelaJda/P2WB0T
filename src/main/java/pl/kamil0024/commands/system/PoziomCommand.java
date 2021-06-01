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

package pl.kamil0024.commands.system;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.SlashContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.util.UserUtil;

import java.util.Objects;

public class PoziomCommand extends Command {

    public PoziomCommand() {
        name = "poziom";
        category = CommandCategory.SYSTEM;
        enabledInRekru = true;
        hideSlash = true;
        commandData = new CommandData(name, name + ".opis")
                .addOption(OptionType.USER, "user", "Użytkownik", true);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Member mem = context.getParsed().getMember(context.getArgs().get(0));
        if (mem == null) mem = context.getMember();

        PermLevel lvl = UserUtil.getPermLevel(mem);
        context.sendTranslate("poziom.send", UserUtil.getName(mem.getUser()),
                lvl.getNumer(), context.getTranslate(lvl.getTranlsateKey())).queue();
        return true;
    }

    @Override
    public boolean execute(@NotNull SlashContext context) {
        Member mem = Objects.requireNonNull(context.getEvent().getOption("user")).getAsMember();
        if (mem == null) mem = context.getMember();
        PermLevel lvl = UserUtil.getPermLevel(mem);
        context.sendTranslate("poziom.send", UserUtil.getName(mem.getUser()),
                lvl.getNumer(), context.getTranslate(lvl.getTranlsateKey()));
        return true;
    }

}
