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

package pl.kamil0024.nieobecnosci;


import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.core.database.NieobecnosciDao;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.nieobecnosci.listeners.NieobecnosciListener;

public class NieobecnosciModule implements Modul {

    private final ShardManager api;
    private final NieobecnosciDao nieobecnosciDao;

    @Getter
    private final String name = "nieobecnosci";

    @Getter
    @Setter
    private boolean start = false;

    private NieobecnosciListener nieobecnosciListener;
    private final NieobecnosciManager nieobecnosciManager;

    public NieobecnosciModule(ShardManager api, NieobecnosciDao nieobecnosciDao, NieobecnosciManager nieobecnosciManager) {
        this.api = api;
        this.nieobecnosciDao = nieobecnosciDao;
        this.nieobecnosciManager = nieobecnosciManager;
    }

    @Override
    public boolean startUp() {
        this.nieobecnosciListener = new NieobecnosciListener(nieobecnosciDao, nieobecnosciManager);
        api.addEventListener(nieobecnosciListener);
        setStart(true);
        return true;
    }

    @Override
    public boolean shutDown() {
        api.removeEventListener(nieobecnosciListener);
        setStart(false);
        return true;
    }

}
