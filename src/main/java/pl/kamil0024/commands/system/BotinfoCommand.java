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

package pl.kamil0024.commands.system;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandManager;
import pl.kamil0024.core.command.SlashContext;
import pl.kamil0024.core.module.ModulManager;
import pl.kamil0024.core.socket.SocketManager;
import pl.kamil0024.core.util.Statyczne;
import pl.kamil0024.core.util.Tlumaczenia;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.moderation.listeners.ModLog;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class BotinfoCommand extends Command {

    private final CommandManager commandManager;
    private final ModulManager modulManager;
    private final SocketManager socketManager;

    public BotinfoCommand(CommandManager commandManager, ModulManager modulManager, SocketManager socketManager) {
        name = "botinfo";
        aliases = Arrays.asList("botstat", "botstats");
        cooldown = 5;
        enabledInRekru = true;
        this.commandManager = commandManager;
        this.modulManager = modulManager;
        this.socketManager = socketManager;
        hideSlash = true;
        commandData = getData();
    }

    @Override
    public boolean execute(@NotNull SlashContext context) {
        EmbedBuilder eb = new EmbedBuilder();
        ArrayList<MessageEmbed.Field> fields = new ArrayList<>();

        long free = Runtime.getRuntime().freeMemory();
        long total = Runtime.getRuntime().totalMemory();
        double used = round((double) (total - free) / 1024 / 1024);
        String format = String.format("%s/%s MB", used, round((double) total / 1024 / 1024));

        fields.add(new MessageEmbed.Field(context.getTranslate("botinfo.ram"), format, false));
        fields.add(new MessageEmbed.Field(context.getTranslate("botinfo.uptime"), new BDate(Statyczne.START_DATE.getTime(), ModLog.getLang()).difference(new Date().getTime()), false));
        fields.add(new MessageEmbed.Field(context.getTranslate("botinfo.jda"), JDAInfo.VERSION, false));
        fields.add(new MessageEmbed.Field(context.getTranslate("botinfo.shard"), String.format("[ %s / %s ]", context.getJDA().getShardInfo().getShardId(), context.getJDA().getShardInfo().getShardTotal()), false));
        fields.add(new MessageEmbed.Field(context.getTranslate("botinfo.core"), Statyczne.WERSJA, false));
        fields.add(new MessageEmbed.Field(context.getTranslate("botinfo.jre"), System.getProperty("java.version"), false));
        fields.add(new MessageEmbed.Field(context.getTranslate("botinfo.os"), System.getProperty("os.name"), false));
        fields.add(new MessageEmbed.Field(context.getTranslate("botinfo.users"), String.valueOf(context.getGuild().getMemberCount()),
                false));
        fields.add(new MessageEmbed.Field(context.getTranslate("botinfo.name"), UserUtil.getFullName(context.getJDA().getSelfUser()), false));
        fields.add(new MessageEmbed.Field(context.getTranslate("botinfo.cmd"), String.valueOf(commandManager.getCommands().size()), false));
        fields.add(new MessageEmbed.Field(context.getTranslate("botinfo.modules"), String.valueOf(modulManager.getModules().size()), false));

        fields.add(new MessageEmbed.Field(context.getTranslate("botinfo.musicbots"), String.format("[ %s ]", socketManager.getClients().size()), false));

        eb.setColor(UserUtil.getColor(context.getMember()));

        int i = 1;
        for (MessageEmbed.Field field : fields) {
            boolean bol = true;
            if (i > 3) {
                i = 0;
                bol = false;
            }
            eb.addField(field.getName(), field.getValue(), bol);
            i++;
        }
        context.getHook().sendMessageEmbeds(eb.build()).queue();
        return true;
    }

    public static double round(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static int calcCPU(long cpuStartTime, long elapsedStartTime, int cpuCount) {
        long end = System.nanoTime();
        long totalAvailCPUTime = cpuCount * (end - elapsedStartTime);
        long totalUsedCPUTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - cpuStartTime;
        float per = ((float) totalUsedCPUTime * 100) / (float) totalAvailCPUTime;
        return (int) per;
    }


}
