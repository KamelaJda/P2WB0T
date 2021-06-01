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
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.util.Tlumaczenia;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public abstract class Command {

    protected String name;
    protected int cooldown = 0;
    protected CommandCategory category = CommandCategory.SYSTEM;
    protected PermLevel permLevel = PermLevel.MEMBER;
    protected List<String> aliases = new ArrayList<>();
    protected CommandData commandData = null;
    protected boolean enabledInRekru = false;
    protected boolean onlyInRekru = false;
    protected boolean hideSlash = false;
    protected final Map<String, Method> subCommands = new HashMap<>();

    protected boolean execute(CommandContext context) {
        throw new UnsupportedOperationException("Komenda nie ma zaimplementowanej funkcji execute(CommandContext)");
    }

    protected boolean execute(SlashContext context) {
        throw new UnsupportedOperationException("Komenda nie ma zaimplementowanej funkcji execute(SlashContext)");
    }

    @Override
    public String toString() {
        return this.name;
    }

    protected CommandData getData() {
        return new CommandData(this.name, Tlumaczenia.get(this.name + ".opis"));
    }

}
