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

package pl.kamil0024.core.database.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.util.UserUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class UserinfoConfig {
    public UserinfoConfig() {}

    public UserinfoConfig(String id) {
        this.id = id;
    }

    private String id = "";

    private String mcNick = null;
    private String username;
    private String tag;
    private PermLevel permLevel = PermLevel.MEMBER;
    private String avatarUrl;
    private boolean inGuild = true;
    private List<String> roles = new ArrayList<>();

    public String getWhateverName() {
        return getMcNick() == null ? getUsername() : getMcNick();
    }

    public static UserinfoConfig convert(Member member) {
        UserinfoConfig inf = convert(member.getUser());
        if (member.getNickname() != null) inf.setMcNick(member.getNickname());
        inf.setPermLevel(UserUtil.getPermLevel(member));
        inf.setInGuild(true);
        inf.setRoles(member.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
        return inf;
    }

    public static UserinfoConfig convert(@Nullable User user) {
        if (user == null) return null;
        UserinfoConfig inf = new UserinfoConfig(user.getId());
        inf.setUsername(user.getName());
        inf.setTag(user.getDiscriminator());
        inf.setAvatarUrl(user.getAvatarUrl());
        inf.setInGuild(false);
        return inf;
    }

}
