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

package pl.kamil0024.antiraid.managers;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.CommandExecute;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.AntiRaidDao;
import pl.kamil0024.core.database.config.AntiRaidConfig;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.redis.Cache;
import pl.kamil0024.core.redis.RedisManager;
import pl.kamil0024.core.util.UserUtil;

import javax.crypto.IllegalBlockSizeException;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class AntiRaidManager {

    private final NormalizedLevenshtein l = new NormalizedLevenshtein();
    private final AntiRaidDao dao;
    private final Cache<List<FakeAntiRaidMessage>> cache;

    public AntiRaidManager(AntiRaidDao dao, RedisManager redis) {
        this.dao = dao;
        this.cache = redis.new CacheRetriever<List<FakeAntiRaidMessage>>(){}.getCache(21600);
    }

    public List<FakeAntiRaidMessage> getMessages(String userId) {
        return cache.getIfPresent(userId);
    }

    public void save(String author, List<FakeAntiRaidMessage> message) {
        cache.put(author, message);
    }

    public void saveMessage(String userId, Message message) {
        List<FakeAntiRaidMessage> lastC = getMessages(userId);
        if (lastC == null || lastC.isEmpty()) {
            List<FakeAntiRaidMessage> arr = new ArrayList<>();
            arr.add(null);
            arr.add(null);
            arr.add(null);
            arr.add(null);
            arr.add(FakeAntiRaidMessage.convert(message));
            lastC = arr;
            save(message.getAuthor().getId(), arr);
        } else {
            lastC.remove(0);
            lastC.add(FakeAntiRaidMessage.convert(message));
            save(message.getAuthor().getId(), lastC);
        }

        List<Double> procentRoznicy = new ArrayList<>();

        for (int i = 0; i < lastC.size(); i++) {
            try {
                if (lastC.get(i) == null || lastC.get(i + 1) == null) {
                    throw new IllegalBlockSizeException();
                }
            } catch (Exception err) {
                continue;
            }
            procentRoznicy.add(l.similarity(lastC.get(i).getContent(), lastC.get(i + 1).getContent()));
        }

        double czulosc = 80 / 100d;
        List<Double> proc = procentRoznicy.stream().filter(v -> v >= czulosc).collect(Collectors.toList());
        if (proc.size() >= 3) {
            sendRaid(message.getAuthor(), lastC, "3 wiadomości o podobieństwie " + proc.stream().map(w -> w * 100 + "%")
                    .collect(Collectors.joining(", ")), message.getGuild());
            return;
        }

        if (message.getMentionedMembers().size() >= 5 ||
                (message.getMentionedRoles().stream().filter(Role::isMentionable).count() == message.getGuild()
                        .getRoles().stream().filter(Role::isMentionable).count() && message.getGuild().getRoles().stream()
                        .anyMatch(Role::isMentionable))) {
            sendRaid(message.getAuthor(), lastC, "5 pingów w wiadomości lub oznaczone wszystkie role oznaczalne", message.getGuild());
            return;
        }

        if (message.getMentionedMembers().stream().filter(m -> UserUtil.getPermLevel(m).getNumer() > PermLevel.MEMBER.getNumer()).count() > 3) {
            sendRaid(message.getAuthor(), lastC, "Oznaczenie +3 członków administracji", message.getGuild());
            return;
        }

        if (message.getMentionedMembers().stream().map(Member::getId).collect(Collectors.toList()).contains("630825972178092038")) {
            sendRaid(message.getAuthor(), lastC, "Oznaczenie administracji", message.getGuild());
        }

    }

    public void sendRaid(User user, List<FakeAntiRaidMessage> messages, String reason, Guild guild) {
        try {
            guild.addRoleToMember(user.getId(), Objects.requireNonNull(guild.getRoleById(Ustawienia.instance.muteRole))).complete();
        } catch (Exception e) {
            Log.newError("Nie udało się ukarać gracza za raida!", getClass());
            Log.newError(e, getClass());
            return;
        }
        save(user.getId(), new ArrayList<>());
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.red);
        eb.setAuthor("Wykryto raida!");
        eb.addField("Użytkownik", UserUtil.getLogName(user), false);
        eb.addField("Kanał ostatniej wiadomości", String.format("<#%s>", messages.get(messages.size() - 1).getChannel()), false);

        eb.addField("", "Ostatnie wiadomości:", false);
        int i = 1;
        StringBuilder dowodMsg = new StringBuilder();
        for (FakeAntiRaidMessage message : messages) {
            try {
                eb.addField("Wiadomość nr:  " + i, message.getContent(), false);
                dowodMsg.append(message.getContent()).append("\n");
            } catch(Exception ignored) {}
            i++;
        }
        eb.addField("Powód", reason, false);

        try {
            Message msg = Objects.requireNonNull(guild.getTextChannelById(Ustawienia.instance.channel.moddc)).sendMessage(eb.build()).complete();

            AntiRaidConfig arc = new AntiRaidConfig();
            arc.setDate(new Date().getTime());
            arc.setUserId(user.getId());
            arc.setId(msg.getId());
            arc.setReason(reason);
            arc.setMessages(dowodMsg.toString());
            dao.save(arc);
            msg.addReaction(CommandExecute.getReaction(msg.getAuthor(), true)).queue();
            msg.addReaction(CommandExecute.getReaction(msg.getAuthor(), false)).queue();
        } catch (Exception e) {
            Log.newError(e, getClass());
        }

    }

    @Data
    @AllArgsConstructor
    public static class FakeAntiRaidMessage {
        private final String messageId;
        private final String content;
        private final long data;
        private final String channel;

        public static FakeAntiRaidMessage convert(Message message) {
            return new FakeAntiRaidMessage(message.getId(), message.getContentRaw(), message.getTimeCreated().toInstant().getEpochSecond(), message.getChannel().getId());
        }

    }

}