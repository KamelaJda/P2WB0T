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

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import pl.kamil0024.core.command.CommandContext;

public abstract class Args {

    @Getter
    protected String name;

    public Object parsed(String o, JDA jda, CommandContext context) {
        throw new UnsupportedOperationException("Argu nie ma zaimplementowanej funkcji execute()");
    }

    @Override
    public String toString() {
        return this.name;
    }

}
