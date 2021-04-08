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

package pl.kamil0024.stats.listener;

import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.stats.StatsModule;

import java.util.Objects;

public class StatsListener extends ListenerAdapter {

    private final StatsModule statsModule;

    public StatsListener(StatsModule statsModule) {
        this.statsModule = statsModule;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        Category category = event.getChannel().getParent();
        if (category == null || event.getAuthor().isBot()) return;

        if (!Objects.requireNonNull(event.getMember()).getRoles().contains(event.getGuild().getRoleById(Ustawienia.instance.roles.chatMod))) return;

        if (category.getId().equals("425673488456482817") || category.getId().equals("506210855231291393") || category.getId().equals("494507499739676686")) {
            statsModule.getStatsCache().addNapisanychWiadomosci(event.getAuthor().getId(), 1);
        }

    }
}
