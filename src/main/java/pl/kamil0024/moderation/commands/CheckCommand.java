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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.database.config.CaseConfig;
import pl.kamil0024.core.util.EmbedPaginator;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.core.util.kary.Dowod;
import pl.kamil0024.core.util.kary.Kara;
import pl.kamil0024.moderation.listeners.ModLog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CheckCommand extends Command {

    private final CaseDao caseDao;
    private final EventWaiter eventWaiter;

    public CheckCommand(CaseDao caseDao, EventWaiter eventWaiter) {
        name = "check";
        permLevel = PermLevel.CHATMOD;
        category = CommandCategory.MODERATION;
        cooldown = 5;
        this.caseDao = caseDao;
        this.eventWaiter = eventWaiter;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        User user = context.getParsed().getUser(context.getArgs().get(0));
        if (user == null) {
            context.send("Nie ma takiego użytkownika!").queue();
            return false;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(UserUtil.getColor(context.getMember()));
        eb.setTimestamp(Instant.now());
        eb.setFooter("Check");
        eb.setThumbnail(user.getAvatarUrl());

        Member mem = context.getParsed().getMember(user.getId());

        Kara kara = null;
        List<CaseConfig> kary = caseDao.getAllDesc(user.getId(), null);
        if (!kary.isEmpty()) kara = kary.get(kary.size() - 1).getKara();

        eb.addField(context.getTranslate("check.name"), UserUtil.getLogName(user), false);
        eb.addField(context.getTranslate("check.ban"), hasBan(user.getId(), context.getGuild()) ? "Tak" : "Nie", false);
        if (mem != null)
            eb.addField(context.getTranslate("check.mute"), MuteCommand.hasMute(mem) ? "Tak" : "Nie", false);
        eb.addField(context.getTranslate("check.onserwer"), mem != null ? "Tak" : "Nie", false);
        eb.addField(context.getTranslate("check.lastcase"), "ID: " + (kara == null ? "???" : kara.getKaraId()), false);

        List<EmbedBuilder> list = new ArrayList<>();
        list.add(eb);
        if (kara != null) list.add(ModLog.getEmbed(kara, context.getShardManager(), false, true));
        if (kara != null && kara.getDowody() != null && !kara.getDowody().isEmpty()) {
            for (Dowod dowod : kara.getDowody()) {
                list.add(DowodCommand.getEmbed(dowod, context));
            }
        }
        new EmbedPaginator(list, context.getUser(), eventWaiter)
                .create(context.getChannel());
        return true;
    }

    public static boolean hasBan(String userId, Guild guild) {
        return guild.retrieveBanList().complete()
                .stream().filter(f -> f.getUser().getId().equals(userId))
                .findFirst().orElse(null) != null;
    }

}
