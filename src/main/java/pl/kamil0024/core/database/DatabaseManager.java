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

package pl.kamil0024.core.database;

import gg.amy.pgorm.PgStore;
import pl.kamil0024.core.Ustawienia;

@SuppressWarnings("unused")
public class DatabaseManager {

    private PgStore pgStore;

    public DatabaseManager() {
    }

    public void shutdown() {
        if (pgStore != null) pgStore.disconnect();
    }

    public PgStore getPgStore() {
        if (pgStore == null) throw new IllegalStateException("pgStore == null");
        return pgStore;
    }

    public void load() {
        Ustawienia ustawienia = Ustawienia.instance;
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            /* lul */
        }
        pgStore = new PgStore(ustawienia.postgres.jdbcUrl, ustawienia.postgres.user, ustawienia.postgres.password);
        pgStore.connect();
    }

}
