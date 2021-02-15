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
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.database.config.Dao;
import pl.kamil0024.core.database.config.TicketConfig;
import pl.kamil0024.core.util.BetterStringBuilder;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.ticket.config.ChannelTicketConfig;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TicketDao implements Dao<TicketConfig> {

    private final PgMapper<TicketConfig> mapper;

    public TicketDao(DatabaseManager databaseManager) {
        if (databaseManager == null) throw new IllegalStateException("databaseManager == null");
        mapper = databaseManager.getPgStore().mapSync(TicketConfig.class);
    }

    @Override
    public TicketConfig get(String id) {
        return mapper.load(id).orElseGet(() -> new TicketConfig(id));
    }

    @Nullable
    public TicketConfig getSpam(String userId) {
        return mapper.getTicketBySpam(userId);
    }

    @Override
    public void save(TicketConfig toCos) {
        mapper.save(toCos);
    }

    private List<TicketConfig> reverse(List<TicketConfig> list) {
        Collections.reverse(list);
        return list;
    }

    @Override
    public List<TicketConfig> getAll() {
        return reverse(mapper.loadAll());
    }

    public List<TicketConfig> getById(String id, int offset) {
        return mapper.getTicketById(id, offset);
    }

    public List<TicketConfig> getByNick(String nick, int offset) {
        return mapper.getTicketByNick(nick, offset);
    }

    public List<TicketConfig> getAllTickets(int offset) {
        return mapper.getAllTickets(offset);
    }

    public List<TicketConfig> getAllTickets(int offset, String admId, boolean read) {
        return mapper.getAllTicketsByFiltr(offset, admId, read);
    }

    public List<TicketConfig> getAllTicketsSpam(int offset) {
        return mapper.getTicketsSpam(offset);
    }

    public synchronized TicketConfig getByRandomId() {
        int n = 12;
        String alphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            int index
                    = (int)(alphaNumericString.length()
                    * Math.random());
            sb.append(alphaNumericString
                    .charAt(index));
        }
        return new TicketConfig(sb.toString());
    }

    public void sendMessage(Member member, Member adm, ChannelTicketConfig conf) {
        try {
            TicketConfig tc = getByRandomId();
            tc.setAdmId(adm.getId());
            String n = UserUtil.getMcNick(adm);
            if (!n.equals("-")) tc.setAdmNick(n);
            tc.setUserId(member.getId());
            long date = new Date().getTime();
            tc.setCreatedTime(date);
            tc.setTimestamp(date - conf.getCreatedTime());
            String nick = UserUtil.getMcNick(member);
            tc.setUserNick(nick.equals("-") ? null : nick);
            tc.setKategoria(conf.getKategoria());
            BetterStringBuilder msg = new BetterStringBuilder();
            msg.appendLine("Cześć,\n");
            msg.appendLine("twoja prośba o pomoc została zamknięta przez administratora! " +
                    "Dbamy o jakość świadczonej przez nas pomocy oraz staramy się polepszać oferowane przez nas usługi, " +
                    "prosimy więc o wypełnienie krótkiej ankiety pod poniższym linkiem:");
            msg.appendLine(TicketConfig.getUrl(tc));
            msg.appendLine("\nZ góry dziękujemy za poświęcony czas!");

            save(tc);
            member.getUser().openPrivateChannel().complete().sendMessage(msg.toString()).complete();
        } catch (Exception ignored) { }
    }

}
