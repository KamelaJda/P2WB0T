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

package pl.kamil0024.status.statusy;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.RichPresence;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.chat.listener.ChatListener;
import pl.kamil0024.core.logger.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WulgarneStatusy extends ListenerAdapter {

    private final List<String> przeklenstwa;

    public WulgarneStatusy() {
        this.przeklenstwa = ChatListener.getPrzeklenstwa();
    }

    public List<String> getAvtivity(Member mem) {
        List<String> list = new ArrayList<>();
        try {
            List<Activity> activities = mem.getActivities();
            activities.removeIf(a -> a.getType() == Activity.ActivityType.LISTENING);
            for (Activity act : activities) {
                list.add(act.getName());
                if (act.isRich()) {
                    RichPresence rp = act.asRichPresence();
                    try {
                        //noinspection ConstantConditions
                        list.add(rp.getState());
                        list.add(rp.getDetails());
                    } catch (NullPointerException ignored) { }
                }
            }
        } catch (Exception e) {
            Log.newError(e, getClass());
        }
        list.removeIf(Objects::isNull);
        return list;
    }

    @Nullable
    public String containsLink(List<String> list) {
        for (String s : list) {
            if (s != null && !s.isEmpty() && !s.toLowerCase().contains("gram na")) {
                s = s.toLowerCase().replaceAll("derpmc.pl", "tak")
                        .replaceAll("feerko\\.pl", "tak")
                        .replaceAll("hajsmc\\.pl", "tak")
                        .replaceAll("roizy\\.pl", "tak")
                        .replaceAll("hypixel\\.net", "tak")
                        .replaceAll("blazingpack\\.pl", "tak")
                        .replaceAll("\\.by", "tak")
                        .replaceAll("blazingpack\\.pl", "tak");
                if (ChatListener.HTTP.matcher(s).matches() || ChatListener.DISCORD_INVITE.matcher(s).matches()) return s;
            }
        }
        return null;
    }

    @Nullable
    public String containsSwear(List<String> list) {
        list.removeIf(Objects::isNull);
        list.removeIf(String::isEmpty);
        for (String s : list) {
            for (String split : s.toLowerCase().split(" ")) {
                if (przeklenstwa.contains(split)) return split;
            }
        }
        return null;
    }

}
