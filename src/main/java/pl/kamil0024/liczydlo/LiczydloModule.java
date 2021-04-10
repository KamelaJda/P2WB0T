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

package pl.kamil0024.liczydlo;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.core.module.Modul;

public class LiczydloModule implements Modul {

    private final ShardManager api;

    @Getter @Setter
    private boolean start = false;
    private LiczydloListener liczydloListener;

    public LiczydloModule(ShardManager api) {
        this.api = api;
    }

    @Override
    public boolean startUp() {
        this.liczydloListener = new LiczydloListener();
        api.addEventListener(liczydloListener);
        setStart(true);
        return true;
    }

    @Override
    public boolean shutDown() {
        api.removeEventListener(liczydloListener);
        setStart(false);
        return true;
    }

    @Override
    public String getName() {
        return "liczydlo";
    }

}
