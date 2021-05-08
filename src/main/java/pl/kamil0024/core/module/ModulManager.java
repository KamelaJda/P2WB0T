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

package pl.kamil0024.core.module;

import lombok.Getter;

import java.util.ArrayList;

public class ModulManager {

    @Getter
    private final ArrayList<Modul> modules;

    public ModulManager() {
        this.modules = new ArrayList<>();
    }

    public void reloadAll() {
        modules.forEach(this::reload);
    }

    public void disableAll() {
        modules.forEach(Modul::shutDown);
    }

    public void startAll() {
        modules.forEach(this::start);
    }

    public void start(Modul modul) {
        if (!modul.isStart()) {
            modul.startUp();
        }
    }

    public void reload(Modul modul) {
        try {
            modul.shutDown();
            modul.startUp();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
