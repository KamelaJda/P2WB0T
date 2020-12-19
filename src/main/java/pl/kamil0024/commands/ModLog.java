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

package pl.kamil0024.commands;

import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import pl.kamil0024.bdate.util.BLanguage;
import pl.kamil0024.commands.moderation.MuteCommand;
import pl.kamil0024.commands.moderation.TempbanCommand;
import pl.kamil0024.commands.moderation.TempmuteCommand;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.database.config.CaseConfig;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.BetterStringBuilder;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.core.util.WebhookUtil;
import pl.kamil0024.core.util.kary.Kara;
import pl.kamil0024.core.util.kary.KaryEnum;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ModLog extends ListenerAdapter {

    private final ShardManager api;
    private final TextChannel modlog;
    private final CaseDao caseDao;

    public ModLog(ShardManager api, CaseDao caseDao) {
        this.api = api;
        this.modlog = api.getTextChannelById(Ustawienia.instance.channel.modlog);
        if (modlog == null) {
            Log.newError("Kanał do modlogów jest nullem", ModLog.class);
            throw new UnsupportedOperationException("Kanał do modlogów jest nullem");
        }
        this.caseDao = caseDao;
        ScheduledExecutorService executorSche = Executors.newScheduledThreadPool(2);
        executorSche.scheduleAtFixedRate(() -> {
            try {
                check();
            } catch (Exception e) {
                Log.newError("Check ci znowu nawalił", getClass());
                Log.newError(e, getClass());
            }
        }, 2, 2, TimeUnit.MINUTES);
    }

    @SneakyThrows
    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {
        if (!event.getGuild().getId().equals(Ustawienia.instance.bot.guildId)) return;
        Thread.sleep(10000);
        List<CaseConfig> cc = caseDao.getAktywe(event.getUser().getId());
        checkKara(event.getMember(), false, cc);
        //checkKara(event, true, caseDao.getNickAktywne(nick));
    }

    public synchronized void checkKara(Member event, boolean nick, List<CaseConfig> cc) {
        for (CaseConfig config : cc) {
            Kara k = config.getKara();
            String powod = null;

            String check = nick ? "Ten nick " : "Te konto ";
            User adm = userExceptionBypass(config.getKara().getAdmId(), api);
            if (adm == null) adm = event.getGuild().getSelfMember().getUser();
            switch (k.getTypKary()) {
                case BAN:
                    powod = check + "jest permanentnie zbanowane!";
                    event.getGuild().ban(event, 0, powod).complete();
                    break;
                case MUTE:
                    powod = check + "jest permanentnie wyciszone";
                    event.getGuild().addRoleToMember(event, Objects.requireNonNull(api.getRoleById(Ustawienia.instance.muteRole))).complete();
                    break;
                case TEMPBAN:
                    powod = check + "jest tymczasowo zbanowane";
                    TempbanCommand.tempban(event, adm, k.getPowod(), k.getDuration(), caseDao, this, true);
                    break;
                case TEMPMUTE:
                    powod = check + "jest tymczasowo wyciszone";
                    TempmuteCommand.tempmute(event, adm, k.getPowod(), k.getDuration(), caseDao, this, true);
                    break;
            }

            if (powod == null) return;

            Kara kara = new Kara();

            kara.setKaraId(Kara.getNextId(caseDao.getAll()));
            kara.setKaranyId(event.getId());
            kara.setMcNick(UserUtil.getMcNick(event));
            kara.setAdmId(k.getAdmId());
            kara.setPowod(powod + " (" + k.getPowod() + ") [ID: " + k.getKaraId() + "]");
            kara.setTimestamp(new Date().getTime());
            kara.setTypKary(k.getTypKary());
            kara.setAktywna(false);

            try {
                kara.setEnd(config.getKara().getEnd());
                kara.setDuration(config.getKara().getDuration());
            } catch (Exception ignored) { }

            if (!k.getPowod().contains("Te konto jest") && k.getKaraId() != 0) {
                if (nick) {
                    kara.setAktywna(true);
                    CaseConfig caseconfig = new CaseConfig(event.getId());
                    caseconfig.setKara(kara);
                    caseDao.save(caseconfig);

                    for (CaseConfig ccase : caseDao.getAktywe(event.getId())) {
                        caseDao.delete(ccase.getKara().getKaraId());
                        ccase.getKara().setAktywna(false);
                        caseDao.save(ccase);
                    }

                }
                sendModlog(kara, true);
                return;
            }
        }
    }

    private void check() {
        Date data = new Date();
        Guild g = api.getGuildById(Ustawienia.instance.bot.guildId);
        Role muteRole = api.getRoleById(Ustawienia.instance.muteRole);
        if (muteRole == null) throw new NullPointerException("muteRole jest nullem");
        if (g == null) throw new NullPointerException("Serwer docelowy jest nullem");
        List<CaseConfig> cc = caseDao.getAllAktywne();
        List<String> filtredBans = new ArrayList<>();

        try {
            for (Guild.Ban ban : g.retrieveBanList().complete()) {
                filtredBans.add(ban.getUser().getId());
            }
        } catch (Exception ignored) { }

        for (CaseConfig a : cc) {
            Kara aCase = a.getKara();
            if (!aCase.getAktywna()) continue;

            KaryEnum typ = aCase.getTypKary();
            if (typ == KaryEnum.TEMPBAN || typ == KaryEnum.TEMPMUTE) {
                Long end = aCase.getEnd();
                if (end == null) {
                    Log.newError("Kara jest TEMPBAN || TEMPMUTE a getEnd() jest nullem ID:" + aCase.getKaraId(), ModLog.class);
                    continue;
                }
                if (end - data.getTime() <= 0) {
                    KaryEnum reverseCase = typ == KaryEnum.TEMPBAN ? KaryEnum.UNBAN : KaryEnum.UNMUTE;
                    try {
                        User u = userExceptionBypass(aCase.getKaranyId(), api);
                        if (u == null) continue;

                        if (!filtredBans.contains(u.getId()) && typ == KaryEnum.TEMPBAN) {
                            String msg = "Nie udało się dać kary UNBAN dla %s (ID: %s) bo: Typ nie ma bana";
                            Log.newError(String.format(msg, u.getId(), aCase.getKaraId()), ModLog.class);
                            continue;
                        }

                        Member m = memberExceptionBypass(u.getId(), g);

                        switch (typ) {
                            case TEMPMUTE:
                                if (m != null) g.removeRoleFromMember(m, muteRole).complete();
                                break;
                            case TEMPBAN:
                                try {
                                    g.unban(aCase.getKaranyId()).complete();
                                } catch (Exception e) {
                                    if (filtredBans.contains(u.getId())) {
                                        Log.newError(e, getClass());
                                    }
                                }
                                break;
                        }
                        Kara kara = new Kara();
                        kara.setKaranyId(aCase.getKaranyId());
                        if (m != null) kara.setMcNick(UserUtil.getMcNick(m));
                        kara.setAdmId(Ustawienia.instance.bot.botId);
                        kara.setPowod("Czas minął. (ID: " + aCase.getKaraId() + ")");
                        kara.setTimestamp(data.getTime());
                        kara.setTypKary(reverseCase);
                        kara.setKaraId(Kara.getNextId(cc));
                        aCase.setAktywna(false);
                        sendModlog(kara);
                        CaseConfig kurwa = new CaseConfig(String.valueOf(aCase.getKaraId()));
                        kurwa.setKara(aCase);
                        caseDao.save(kurwa);
                        a.getKara().setAktywna(false);
                        caseDao.save(a);
                    } catch (Exception e) {
                        e.printStackTrace();
                        String msg = "Nie udało się dać kary %s dla %s (ID: %s) bo: %s";
                        Log.newError(String.format(msg, reverseCase, aCase.getKaranyId(), aCase.getKaraId(), e.getMessage()), getClass());
                    }
                    if (typ == KaryEnum.TEMPMUTE) {
                        if (userExceptionBypass(aCase.getKaranyId(), api) != null) {
                            Member m = memberExceptionBypass(aCase.getKaranyId(), g);
                            if (m != null && MuteCommand.hasMute(m)) {
                                Log.newError("Uzytkownik " + UserUtil.getFullName(m.getUser()) + " dostal unmuta, ale nadal ma range Wyciszony!", getClass());
                            }
                        }
                    }
                }
            }
        }
    }

    public synchronized void sendModlog(Kara kara) {
        sendModlog(kara, true);
    }

    public synchronized void sendModlog(Kara kara, boolean sendDm) {
        Message msg = modlog.sendMessage(getEmbed(kara, api, sendDm).build()).complete();
        String url = msg.getJumpUrl().replaceAll("https://discord(app)?.com/channels/", "");

        CaseConfig cc = caseDao.get(kara.getKaraId());
        if (cc.getKara() != null) {
            cc.getKara().setMessageUrl(url);
            caseDao.save(cc);
        }
    }

    public static EmbedBuilder getEmbed(Kara kara, ShardManager api) {
        return getEmbed(kara, api, false);
    }

    public static EmbedBuilder getEmbed(Kara kara, ShardManager api, Boolean bol) {
        return getEmbed(kara, api, bol, false);
    }

    public static EmbedBuilder getEmbed(Kara kara, ShardManager api, Boolean bol, Boolean seeAktywna) {
        SimpleDateFormat sfd = new SimpleDateFormat("dd.MM.yyyy `@` HH:mm:ss");
        EmbedBuilder eb = new EmbedBuilder();
        User u = api.retrieveUserById(kara.getKaranyId()).complete();
        Member mem = memberExceptionBypass(kara.getAdmId(), api.getGuildById(Ustawienia.instance.bot.guildId));
        User admUser = userExceptionBypass(kara.getAdmId(), api);
        String adm = "Nie można było pobrać administratora. Jego ID to: " + kara.getAdmId();

        if (mem != null) eb.setColor(UserUtil.getColor(mem));
        eb.addField("Osoba karana", MarkdownSanitizer.escape(UserUtil.getFullName(u)), false);
        eb.addField("Nick w mc", MarkdownSanitizer.escape(kara.getMcNick()), false);
        eb.addField("Administrator",
                mem != null ? UserUtil.getFullName(mem.getUser()) : (admUser != null ? UserUtil.getLogName(admUser) : adm),
                false);
        eb.addField("Powód", kara.getPowod(), false);
        if (u.getAvatarUrl() != null) eb.setThumbnail(u.getAvatarUrl());
        eb.addField("Nadano o", sfd.format(new Date(kara.getTimestamp())), false);
        if (kara.getTypKary() == KaryEnum.TEMPBAN || kara.getTypKary() == KaryEnum.TEMPMUTE) {
            eb.addField("Kończy się o", sfd.format(new Date(kara.getEnd())), false);
            eb.addField("Nadano na czas", kara.getDuration(), false);
        }
        eb.addField("Typ & ID kary", KaryEnum.getName(kara.getTypKary()) +  " | " + kara.getKaraId(), false);

        if (bol || kara.getTypKary() == KaryEnum.UNBAN || kara.getTypKary() == KaryEnum.UNMUTE) {
            if (bol) {
                try {
                    BetterStringBuilder sb = new BetterStringBuilder();
                    sb.appendLine("Cześć!\n");
                    if (kara.getTypKary() == KaryEnum.UNBAN || kara.getTypKary() == KaryEnum.UNMUTE) {
                        String polski = kara.getTypKary() == KaryEnum.UNBAN ? "odbanowany" : "odciszony";
                        sb.appendLine("Właśnie zostałeś(-aś) **" + polski + "** z powodem **" + kara.getPowod() + "**");
                    } else {
                        sb.append("Właśnie dostałeś(-aś) karę **" + KaryEnum.getName(kara.getTypKary()) + "** z powodem **" +
                                kara.getPowod() + "**");
                        if (kara.getTypKary() == KaryEnum.TEMPBAN || kara.getTypKary() == KaryEnum.TEMPMUTE) {
                            sb.appendLine(" na czas **" + kara.getDuration() + "**");
                        }
                    }
                    sb.append("\n");
                    sb.appendLine("Czas nadania: " + sfd.format(new Date(kara.getTimestamp())));
                    sb.appendLine("ID kary: " + kara.getKaraId());
                    sb.append("--------------------------");
                    u.openPrivateChannel().complete().sendMessage(sb.build()).complete();
                    eb.addField("Udało się wysłać wiadomość o karze na pw?", "Tak", false);
                } catch (Exception e) {
                    eb.addField("Udało się wysłać wiadomość o karze na pw?", "Nie", false);
                }
            }
        } else {
            if (kara.getMessageUrl() != null) {
                eb.addField("Kolega prosi o linka?", "[Klik](" + "https://discord.com/channels/" + kara.getMessageUrl() + ")", false);
            }
            if (seeAktywna) eb.addField("Aktywna?", kara.getAktywna() ? "Tak" : "Nie", false);
            if (kara.getDowody() != null && kara.getDowody().size() != 0) {
                eb.addField("Dowody", "Do tej kary dołączono dowody. Aby je wyświetlić użyj komendy `dowod " + kara.getKaraId() + " list`", false);
            }
        }
        return eb;
    }

    public static BLanguage getLang() {
        BLanguage lang = new BLanguage();
        lang.setDay("d.");
        lang.setHour("gdz.");
        lang.setMinute("min.");
        lang.setSecond("sek.");
        return lang;
    }

    @Override
    public void onGuildBan(@Nonnull GuildBanEvent event) {
        sendCase(event.getGuild(), event.getUser(), ActionType.BAN);
    }

    @Override
    public void onGuildUnban(@Nonnull GuildUnbanEvent event) {
        sendCase(event.getGuild(), event.getUser(), ActionType.UNBAN);
    }

    private void sendCase(Guild guild, User banowany, ActionType at) {
        String odpowiedzialny = "??";
        String powod = "??";
        try {
            List<AuditLogEntry> entries = guild.retrieveAuditLogs().type(at).complete();
            for (AuditLogEntry e : entries) {
                if (e.getTimeCreated().isAfter(OffsetDateTime.now().minusSeconds(15)) &&
                        e.getTargetIdLong() == banowany.getIdLong()) {
                    if (e.getUser() == null) continue;
                    odpowiedzialny = UserUtil.getLogName(e.getUser());
                    powod = e.getReason();
                    break;
                }
            }
        } catch (Exception ignored) { }

        String action = at == ActionType.BAN ? "zbanował" : "odbanował";
        String format = "%s %s %s za `%s`";
        WebhookUtil web = new WebhookUtil();
        web.setType(WebhookUtil.LogType.CASES);
        web.setMessage(String.format(format, odpowiedzialny, action, UserUtil.getLogName(banowany), powod));
        web.send();
    }

    @Nullable
    private static Member memberExceptionBypass(String id, Guild guild) {
        try {
            return guild.retrieveMemberById(id).complete();
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static User userExceptionBypass(String id, ShardManager api) {
        try {
            return api.retrieveUserById(id).complete();
        } catch (Exception e) {
            return null;
        }
    }

}
