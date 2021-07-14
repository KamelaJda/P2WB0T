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

import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.SlashContext;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.util.UserUtil;

public class KanalCommand extends Command {

    public KanalCommand() {
        name = "kanal";
        cooldown = 5;
        hideSlash = true;
        permLevel = PermLevel.CHATMOD;
        commandData = getData().addOption(OptionType.CHANNEL, "kanal", "Kanał, który chcesz sprawdzić", true);
    }

    public boolean execute(SlashContext context) {
        GuildChannel kanal = context.getEvent().getOption("kanal").getAsGuildChannel();
        PermissionOverride override = kanal.getMemberPermissionOverrides().stream()
                .filter(f -> !f.getMember().getId().equals(context.getGuild().getSelfMember().getId())).
                        findFirst().orElse(null);
        if (override == null || override.getMember() == null) {
            context.send("Nikt nie jest właścicielem kanału??");
            return true;
        }
        context.send("Właścicielem kanału jest " + UserUtil.getLogName(override.getMember()));
        return true;
    }

}
