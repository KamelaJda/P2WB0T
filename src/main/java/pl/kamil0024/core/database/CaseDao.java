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

package pl.kamil0024.core.database;

import gg.amy.pgorm.PgMapper;
import pl.kamil0024.core.database.config.CaseConfig;
import pl.kamil0024.core.database.config.Dao;
import pl.kamil0024.core.util.kary.KaryEnum;

import java.util.ArrayList;
import java.util.List;

public class CaseDao implements Dao<CaseConfig> {

    private final PgMapper<CaseConfig> mapper;

    public CaseDao(DatabaseManager databaseManager) {
        if (databaseManager == null) throw new IllegalStateException("databaseManager == null");
        mapper = databaseManager.getPgStore().mapSync(CaseConfig.class);
    }

    @Override
    public CaseConfig get(String id) {
        return mapper.load(id).orElseGet(() -> new CaseConfig(id));
    }

    public CaseConfig get(int id) {
        return mapper.load(String.valueOf(id)).orElseGet(() -> new CaseConfig(String.valueOf(id)));
    }

    @Override
    public synchronized void save(CaseConfig kara) {
        mapper.save(kara);
    }

    @Override
    public List<CaseConfig> getAll() {
        return mapper.loadAll();
    }

    public List<CaseConfig> getAktywe(String id) {
        List<CaseConfig> aktywne = mapper.getAktywne(id);
        aktywne.removeIf(k -> k.getKara().getTypKary() == KaryEnum.UNBAN || k.getKara().getTypKary() == KaryEnum.UNMUTE);
        return mapper.getAktywne(id);
    }

    public List<CaseConfig> getNickAktywne(String nick) {
        if (nick.equals("-")) return new ArrayList<>();
        List<CaseConfig> tak = mapper.getMcAktywne(nick);
        tak.removeIf(k -> k.getKara().getTypKary() == KaryEnum.UNBAN || k.getKara().getTypKary() == KaryEnum.UNMUTE);
        return mapper.getMcAktywne(nick);
    }

    public List<CaseConfig> getAllNick(String nick) {
        if (nick.equals("-")) return new ArrayList<>();
        return mapper.getAllNick(nick);
    }

    public List<CaseConfig> getAllAktywne() {
        return mapper.getAllAktywne();
    }

    public void delete(int id) {
        mapper.delete(id);
    }
    public void remove(int id) { delete(id); }

    public List<CaseConfig> getAllPunAktywne(String id) {
        return mapper.getAllPunAktywne(id);
    }

    public List<CaseConfig> getAll(String userId) {
        return mapper.getAll(userId);
    }

    public List<CaseConfig> getAllDesc(String userId, int offset) {
        return mapper.getDescKary(userId, offset);
    }

    public List<CaseConfig> getAllByOffset(int offset) {
        return mapper.getAllByOffset(offset);
    }

}
