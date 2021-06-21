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

package pl.kamil0024.status.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.logger.Log;

public class ChangeNickname extends ListenerAdapter {

    public ChangeNickname() { }

    @Override
    public void onUserUpdateName(@NotNull UserUpdateNameEvent e) {
        Guild g = e.getJDA().getGuildById(Ustawienia.instance.bot.guildId);
        if (g == null) {
            Log.newError("gildia jest nullem", getClass());
            throw new NullPointerException("gilda jest nullem");
        }

        Member mem = g.retrieveMemberById(e.getUser().getId()).complete();
        if (mem == null) return;

        if (mem.getNickname() == null && mem.getRoles().size() >= 1) {
            try {
                g.modifyNickname(mem, e.getOldName()).complete();
            } catch (Exception ex) {
                Log.error("Nie udało się zmienić nicku!", getClass());
                Log.newError(ex, getClass());
            }
        }

    }
}