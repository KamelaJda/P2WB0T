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

package pl.kamil0024.core.arguments;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.command.CommandContext;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UserArgument extends Args {

    private static final Pattern MENTION_REGEX = Pattern.compile("<@!?(\\d{17,18})>");
    private static final Pattern TAG_REGEX = Pattern.compile("(.{2,32}#\\d{4})");

    public UserArgument() {
        name = "user";
    }

    @Override
    public User parsed(String s, @Nullable JDA jda, CommandContext context) {
        if (jda == null) throw new UnsupportedOperationException("jda==null");
        String prawieUser = String.valueOf(s);
        if (prawieUser == null) return null;
        try {
            try { //NOSONAR
                if (jda.retrieveUserById(prawieUser).complete() != null)
                    return jda.retrieveUserById(prawieUser).complete();
            } catch (Exception ignored) { }
            Matcher matcher = MENTION_REGEX.matcher(prawieUser);
            if (matcher.matches()) {
                return jda.retrieveUserById(matcher.group(1)).complete();
            }
            Matcher matcher1 = TAG_REGEX.matcher(prawieUser);
            if (matcher1.matches()) {
                List<User> ul = jda.getUsers().stream().filter(u -> u.getAsTag().equals(matcher1.group(1)))
                        .collect(Collectors.toList());
                if (ul.size() == 1) return ul.get(0);
            }
        } catch (Exception ignored) { }
        return null;
    }

}
