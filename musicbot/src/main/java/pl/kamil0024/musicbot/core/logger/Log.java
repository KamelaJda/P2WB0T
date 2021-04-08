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

package pl.kamil0024.musicbot.core.logger;

import org.jetbrains.annotations.Nullable;
import pl.kamil0024.musicbot.core.util.WebhookUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Log {

    public Log() { }

    public static void newError(String msg, @Nullable Object... args) {
        WebhookUtil web = new WebhookUtil();
        web.setMessage(String.format(msg, args));
        web.setType(WebhookUtil.LogType.ERROR);
        web.send();

        error(msg, args);
    }

    public static void newError(Throwable e) {
        StringWriter er = new StringWriter();
        e.printStackTrace(new PrintWriter(er));

        boolean strona = false;
        StringBuilder sb = new StringBuilder();
        sb.append("```\n");
        for (String s : er.toString().split("\n")) {
            sb.append(s);
            if (sb.toString().toLowerCase().length() >= 1800) {
                sb.append("\n```");
                WebhookUtil web = new WebhookUtil();
                web.setType(WebhookUtil.LogType.ERROR);
                web.setMessage(sb.toString().replaceAll(" {4}at ", ""));
                web.send();
                sb = new StringBuilder();
                strona = true;
            }
        }
        if (!strona) {
            WebhookUtil web = new WebhookUtil();
            web.setType(WebhookUtil.LogType.ERROR);
            web.setMessage(sb.append("\n```").toString().replaceAll(" {4}at ", "") + "\n```");
            web.send();
        }
    }

    public static void info(String msg, @Nullable Object... args) {
        log("INFO", msg, args);
    }

    public static void warn(String msg, @Nullable Object... args) {
        log("WARN", msg, args);
    }

    public static void error(String msg, @Nullable Object... args) {
        log("ERROR", msg, args);
    }

    public static void debug(String msg, @Nullable Object... args) {
        log("DEBUG", msg, args);
    }

    private static void log(String type, String msg, @Nullable Object... args) {
        String time = new SimpleDateFormat("dd.MM HH:mm:ss").format(Calendar.getInstance().getTime());
        System.out.println(String.format("[%s] [%s] %s", time, type, String.format(msg, args)));
    }

}
