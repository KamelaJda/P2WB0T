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

package pl.kamil0024.moderation.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.moderation.listeners.ModLog;
import pl.kamil0024.commands.system.HelpCommand;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.CommandExecute;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.database.config.CaseConfig;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.*;
import pl.kamil0024.core.util.kary.Dowod;
import pl.kamil0024.core.util.kary.Kara;
import pl.kamil0024.core.util.kary.KaryEnum;
import pl.kamil0024.core.util.kary.KaryJSON;
import pl.kamil0024.stats.StatsModule;

import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class PunishCommand extends Command {

    private final KaryJSON karyJSON;
    private final EventWaiter eventWaiter;
    private final CaseDao caseDao;
    private final ModLog modLog;
    private final StatsModule statsModule;

    public PunishCommand(KaryJSON karyJSON, EventWaiter eventWaiter, CaseDao caseDao, ModLog modLog, StatsModule statsModule) {
        name = "punish";
        aliases.add("pun");
        permLevel = PermLevel.CHATMOD;
        category = CommandCategory.MODERATION;
        this.karyJSON = karyJSON;
        this.eventWaiter = eventWaiter;
        this.caseDao = caseDao;
        this.modLog = modLog;
        this.statsModule = statsModule;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (karyJSON.getKary().isEmpty()) {
            context.send("karyJSON.getKary() jest puste").queue();
            return false;
        }
        String arg = context.getArgs().get(0);
        if (arg == null) {
            context.send(HelpCommand.getUsage(context).build()).queue();
            return false;
        }
        if (arg.equalsIgnoreCase("info")) {
            String arg1 = context.getArgs().get(1);
            if (arg1 == null) {
                List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
                for (EmbedBuilder embedBuilder : getKaraList(karyJSON, context.getMember())) {
                    pages.add(new FutureTask<>(() -> embedBuilder));
                }
                new DynamicEmbedPageinator(pages, context.getUser(), eventWaiter, context.getJDA(), 500).create(context.getChannel(), context.getMessage());
            } else {
                Integer liczba = context.getParsed().getNumber(context.getArgs().get(1));
                if (liczba == null || liczba > karyJSON.getKary().size() || liczba <= 0) {
                    boolean powod = false;
                    for (KaryJSON.Kara kara : karyJSON.getKary()) {
                        if (kara.getPowod().toLowerCase().contains(context.getArgs().get(1).toLowerCase())) {
                            liczba = kara.getId();
                            powod = true;
                            break;
                        }
                    }
                    if (!powod) {
                        context.send("Nie ma takiego numeru!").queue();
                        return false;
                    }
                }
                KaryJSON.Kara kara = karyJSON.getKary().get(liczba - 1);
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(UserUtil.getColor(context.getMember()));
                eb.setDescription("Ilość tierów związanych z `" + kara.getPowod() + "`: " + kara.getTiery().size());

                BetterStringBuilder sb = new BetterStringBuilder();
                sb.appendLine("```md");
                sb.appendLine("Nr. | Ilość warnów | Typ kary | Czas kary");
                int a = 1;
                for (KaryJSON.Tiery tiery : kara.getTiery()) {
                    sb.append(appendLine(a + "", 3).append(" | "));
                    sb.append(appendLine("    " + tiery.getMaxWarns(), 12).append(" | "));
                    sb.append(appendLine(KaryEnum.getName(tiery.getType()), 8).append(" | "));
                    sb.appendLine(new StringBuilder(tiery.getDuration()));
                    a++;
                }
                sb.append("```");
                eb.addField(" ", sb.build(), false);
                context.send(eb.build()).queue();
            }
            return true;
        }

        Integer numer = context.getParsed().getNumber(context.getArgs().get(1));

        ArrayList<Member> osoby = new ArrayList<>();
        Message msg = context.send("Ładuje...").reference(context.getMessage()).complete();

        if (arg.contains(",")) {
            for (String s : arg.split(",")) {
                Member tak = context.getParsed().getMember(s);
                if (tak != null && check(context, tak)) {
                    if (!osoby.contains(tak)) osoby.add(tak);
                }
            }

        } else {
            Member mem = context.getParsed().getMember(arg);
            if (mem != null) osoby.add(mem);
        }

        StringBuilder sb = new StringBuilder();
        for (Member member : osoby) {
            sb.append(UserUtil.getMcNick(member, true)).append("`,` ");
        }

        if (osoby.isEmpty()) {
            msg.editMessage("Nie masz osób do ukarania! Przyczyn może być wiele, te najczęściej spotykany błąd to " +
                    "osoby są już wyciszone, bądź nie ma ich na serwerze").queue();
            return false;
        }

        if (numer == null) {
            List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();
            for (EmbedBuilder embedBuilder : getKaraList(karyJSON, context.getMember(), osoby)) {
                pages.add(new FutureTask<>(() -> embedBuilder));
            }
            new DynamicEmbedPageinator(pages, context.getUser(), eventWaiter, context.getJDA(), 120)
                    .setPun(true)
                    .create(msg);
            initWaiter(context, msg, osoby, context.getMessage(), eventWaiter, karyJSON, caseDao, modLog, statsModule);
            return true;
        }

        if (numer - 1 > karyJSON.getKary().size() || numer <= 0) {
            context.send("Nie ma takiego numeru!").queue();
            return false;
        }
        KaryJSON.Kara kara = karyJSON.getKary().get(numer - 1);
        msg.editMessage(context.getTranslate("punish.wait", sb.toString(), kara.getPowod())).queue();
        Emote red = CommandExecute.getReaction(context.getUser(), false);
        Emote green = CommandExecute.getReaction(context.getUser(), true);
        msg.addReaction(Objects.requireNonNull(green)).queue();
        msg.addReaction(Objects.requireNonNull(red)).queue();
        //noinspection ConstantConditions
        eventWaiter.waitForEvent(MessageReactionAddEvent.class,
                (event) -> event.getUser().getId().equals(context.getUser().getId()) && event.getMessageId().equals(msg.getId()),
                (event) -> {
            try {
                if (event.getReactionEmote().getId().equals(red.getId())) {
                    context.getMessage().delete().complete();
                    msg.delete().complete();
                    return;
                }
                if (event.getReactionEmote().getId().equals(green.getId())) {
                    putPun(kara, osoby, context.getMember(), context.getChannel(), caseDao, modLog, statsModule, null, eventWaiter);
                    try {
                        context.getMessage().delete().complete();
                        event.getChannel().retrieveMessageById(event.getMessageId()).complete().delete().complete();
                    } catch (Exception ignored) {}
                }
                msg.clearReactions().complete();
            } catch (Exception ignored) {}
            },
                30, TimeUnit.SECONDS,
                () -> {
            try {
                msg.getChannel().sendMessage(String.format("<@%s>, twój czas na odpowiedź minął!", context.getUser().getId())).queue();
                msg.delete().queue();
            } catch (Exception ignored) { }
        });
        return true;
    }

    private static boolean check(CommandContext context, Member osoba) {
        return Kara.check(context, osoba.getUser()) == null && !MuteCommand.hasMute(osoba);
    }

    public static void putPun(KaryJSON.Kara kara, List<Member> osoby, Member member, TextChannel txt, CaseDao caseDao, ModLog modLog, StatsModule statsModule, @Nullable Dowod dowod, @Nullable EventWaiter eventWaiter) {
        for (Member osoba : osoby) {
            int jegoWarny = 1;
            List<CaseConfig> cc = caseDao.getAllPunAktywne(osoba.getId());
            cc.removeIf(caseConfig -> !caseConfig.getKara().getPowod().equalsIgnoreCase(kara.getPowod()));
            jegoWarny += cc.size();
            KaryJSON.Tiery jegoTier = null;
            for (KaryJSON.Tiery tiery : kara.getTiery()) {
                if (tiery.getMaxWarns() == jegoWarny) { jegoTier = tiery; }
            }
            if (jegoTier == null && kara.getTiery().get(kara.getTiery().size() - 1).getMaxWarns() >= jegoWarny) {
                jegoTier = kara.getTiery().get(kara.getTiery().size() - 1);
            }
            if (jegoTier == null) {
                jegoTier = kara.getTiery().get(0);
                for (CaseConfig aCase : cc.stream().filter(ka -> ka.getKara().getPunAktywna()).collect(Collectors.toList())) {
                    aCase.getKara().setPunAktywna(false);
                    caseDao.save(aCase);
                }
            }

            if (!member.getId().equals(Ustawienia.instance.bot.botId) && !txt.getId().equals(Ustawienia.instance.channel.moddc)) {
                String msg = "Daje karę **%s** dla **%s** za **%s** na czas **%s**";
                txt.sendMessage(String.format(msg, KaryEnum.getName(jegoTier.getType()), UserUtil.getMcNick(osoba), kara.getPowod(), jegoTier.getDuration()))
                        .queue(m -> m.delete().queueAfter(8, TimeUnit.SECONDS));
            }

            Kara karaBuilder = new Kara();
            karaBuilder.setKaranyId(osoba.getId());
            karaBuilder.setMcNick(UserUtil.getMcNick(osoba));
            karaBuilder.setPunAktywna(true);
            karaBuilder.setTypKary(jegoTier.getType());
            karaBuilder.setPowod(kara.getPowod());
            karaBuilder.setAdmId(member.getUser().getId());
            karaBuilder.setTimestamp(new Date().getTime());
            if (jegoTier.getType() == KaryEnum.TEMPBAN || jegoTier.getType() == KaryEnum.TEMPMUTE) {
                Long dur = new Duration().parseLong(jegoTier.getDuration());
                karaBuilder.setEnd(dur);
                karaBuilder.setDuration(jegoTier.getDuration());
            }
            if (dowod != null) karaBuilder.setDowody(Collections.singletonList(dowod));

            switch (jegoTier.getType()) {
                case KICK:
                    member.getGuild().kick(osoba, kara.getPowod()).queue();
                    statsModule.getStatsCache().addWyrzuconych(member.getId(), 1);
                    break;
                case BAN:
                    member.getGuild().ban(osoba, 0, kara.getPowod()).queue();
                    statsModule.getStatsCache().addZbanowanych(member.getId(), 1);
                    break;
                case MUTE:
                    member.getGuild().addRoleToMember(osoba, Objects.requireNonNull(member.getGuild().getRoleById(Ustawienia.instance.muteRole))).queue();
                    statsModule.getStatsCache().addZmutowanych(member.getId(), 1);
                    break;
                case TEMPBAN:
                    TempbanCommand.tempban(osoba.getUser(), member.getUser(), kara.getPowod(), jegoTier.getDuration(), caseDao, modLog, true, osoba.getGuild(), UserUtil.getMcNick(osoba));
                    statsModule.getStatsCache().addZbanowanych(member.getId(), 1);
                    break;
                case TEMPMUTE:
                    statsModule.getStatsCache().addZmutowanych(member.getId(), 1);
                    String mute = TempmuteCommand.tempmute(osoba, member.getUser(), kara.getPowod(), jegoTier.getDuration(), caseDao, modLog, true);
                    if (mute != null) {
                        if (mute.equalsIgnoreCase("Ta osoba jest już wyciszona!")) {
                            String msg = "Uzytkownik %s chcial wyciszyc %s, ale ten ma juz muta!";
                            Log.newError(String.format(msg, UserUtil.getLogName(member), UserUtil.getLogName(osoba)), PunishCommand.class);
                        }
                        return;
                    }
                    break;
                default:
                    Log.newError("Typ " + jegoTier.getType().name() + " nie jest wpisany!", PunishCommand.class);
            }
            if (osoby.size() == 1 && eventWaiter != null) {
                Kara.put(caseDao, karaBuilder, modLog, eventWaiter, member.getId(), txt, caseDao);
            } else Kara.put(caseDao, karaBuilder, modLog);
        }
    }

    private static void initWaiter(CommandContext context, Message msg, List<Member> osoby, Message userMsg, EventWaiter eventWaiter, KaryJSON karyJSON, CaseDao caseDao, ModLog modLog, StatsModule statsModule) {
        AtomicBoolean kurwaBylaAkcja = new AtomicBoolean(false);
        eventWaiter.waitForEvent(
                GuildMessageReceivedEvent.class,
                (event) -> event.getAuthor().getId().equals(context.getUser().getId()) &&
                        event.getChannel().getId().equals(context.getChannel().getId()),
                (event) -> {
                    kurwaBylaAkcja.set(true);
                    Integer liczba = context.getParsed().getNumber(event.getMessage().getContentRaw());
                    if (liczba == null || liczba - 1 > karyJSON.getKary().size() || liczba <= 0) return;
                    KaryJSON.Kara kara = karyJSON.getKary().get(liczba - 1);
                    putPun(kara, osoby, context.getMember(), context.getChannel(), caseDao, modLog, statsModule, null, eventWaiter);
                    try {
                        event.getMessage().delete().complete();
                        msg.delete().complete();
                        userMsg.delete().complete();
                    } catch (Exception ignored) { }
                },
                3, TimeUnit.MINUTES,
                () -> {
                    if (!kurwaBylaAkcja.get()) {
                        try {
                            msg.getChannel().sendMessage(String.format("<@%s>, twój czas na odpowiedź minął!", context.getUser().getId())).queue();
                            msg.delete().complete();
                        } catch (Exception ignored) { }
                    }
                }
        );
    }
    public static List<EmbedBuilder> getKaraList(KaryJSON karyJSON, Member mem) {
        return getKaraList(karyJSON, mem, null);
    }

    public static List<EmbedBuilder> getKaraList(KaryJSON karyJSON, Member mem, @Nullable ArrayList<Member> osoby) {
        List<EmbedBuilder> pages = new ArrayList<>();
        BetterStringBuilder sb = new BetterStringBuilder();

        if (osoby != null && !osoby.isEmpty()) {
            sb.append("Chcesz ukarać: ");
            for (Member member : osoby) {
                sb.append(member.getAsMention()).append("`,` ");
            }
            sb.append("\n");
        }

        sb.appendLine("```md");
        sb.appendLine("0. Anuluj akcje");
        int size = 0;
        for (KaryJSON.Kara kara : karyJSON.getKary()) {
            sb.appendLine(kara.getId() + ". " + kara.getPowod());
            if (sb.toString().length() >= 950 || karyJSON.getKary().size() == size + 1) {
                sb.appendLine("```");
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(UserUtil.getColor(mem));
                eb.setDescription(sb.toString());
                pages.add(eb);
                sb = new BetterStringBuilder();
                sb.appendLine("```md");
            }
            size++;
        }

        return pages;
    }

    private StringBuilder appendLine(String s, int size) {
        StringBuilder tak = new StringBuilder(s);
        while (tak.toString().length() != size) {
            tak.append(" ");
            if (tak.toString().length() > size) break;
        }
        return tak;
    }

}