/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.kamil0024.core.command;

import lombok.Getter;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Command {

    @Getter
    protected String name;
    @Getter
    protected int cooldown = 0;
    @Getter
    protected CommandCategory category = CommandCategory.SYSTEM;
    @Getter
    protected PermLevel permLevel = PermLevel.MEMBER;
    @Getter
    protected List<String> aliases = new ArrayList<>();
    @Getter
    protected boolean enabledInRekru = false;
    @Getter
    protected boolean onlyInRekru = false;

    @Getter
    protected final Map<String, Method> subCommands = new HashMap<>();

    protected boolean execute(CommandContext context) {
        throw new UnsupportedOperationException("Komenda nie ma zaimplementowanej funkcji execute()");
    }

    @Override
    public String toString() {
        return this.name;
    }
}
