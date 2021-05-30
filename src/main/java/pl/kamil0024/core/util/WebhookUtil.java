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

package pl.kamil0024.core.util;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.Ustawienia;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

@SuppressWarnings("unused")
@Data
public class WebhookUtil {

    private static final String time = new SimpleDateFormat("MM.dd HH:mm:ss").format(Calendar.getInstance().getTime());

    private LogType type;
    private String name;
    private String message;
    private String avatar;
    private String url;
    private WebhookEmbed embed;

    public WebhookUtil() { }

    public void send() {
        if (type == null) throw new NullPointerException("type == null");
        WebhookClient client = WebhookClient.withUrl(getType().url);
        WebhookMessageBuilder builder = new WebhookMessageBuilder();

        if (getName() == null) builder.setUsername(getType().slownie);
        else builder.setUsername(getName());

        if (avatar != null) builder.setAvatarUrl(getAvatar());
        if (!getMessage().isEmpty()) builder.setContent(String.format("[%s] %s", time, getMessage()));
        if (getEmbed() != null) builder.addEmbeds(getEmbed());
        client.send(builder.build());
        client.close();
    }

    public void sendNormalMessage(@Nullable InputStream inputStream) {
        if (getUrl() == null) throw new NullPointerException("getUrl() == null");
        WebhookClient client = WebhookClient.withUrl(getUrl());
        WebhookMessageBuilder builder = new WebhookMessageBuilder();

        if (name != null) builder.setUsername(getName());
        if (avatar != null) builder.setAvatarUrl(getAvatar());
        if (!getMessage().isEmpty()) builder.setContent(getMessage());
        if (getEmbed() != null) builder.addEmbeds(getEmbed());
        if (inputStream != null) builder.addFile("attachment.png", inputStream);
        client.send(builder.build());
        client.close();
    }

    @SuppressWarnings("ConstantConditions")
    public static WebhookEmbed getWebhookEmbed(EmbedBuilder eb) {
        MessageEmbed msg = eb.build();
        WebhookEmbedBuilder builder = new WebhookEmbedBuilder();
        try {
            if (msg.getAuthor().getName() != null) builder.setAuthor(
                    new WebhookEmbed.EmbedAuthor(msg.getAuthor().getName(), msg.getAuthor().getIconUrl(), msg.getAuthor().getUrl())
            );
        } catch (Exception ignored) { }
        try {
            if (msg.getFooter().getText() != null) {
                builder.setFooter(new WebhookEmbed.EmbedFooter(msg.getFooter().getText(), msg.getFooter().getIconUrl()));
            }
        } catch (Exception ignored) { }
        try {
            builder.setImageUrl(msg.getImage().getUrl());
        } catch (Exception ignored) { }

        try {
            msg.getFields().forEach(f -> builder.addField(getField(f)));
        } catch (Exception ignored) { }

        try {
            builder.setTitle(new WebhookEmbed.EmbedTitle(msg.getTitle(), msg.getUrl()));
        } catch (Exception ignored) { }

        try {
            builder.setDescription(msg.getDescription());
        } catch (Exception ignored) { }

        try {
            builder.setColor(msg.getColor().getRGB());
        } catch (Exception ignored) { }

        try {
            builder.setTimestamp(msg.getTimestamp());
        } catch (Exception ignored) { }

        try {
            builder.setThumbnailUrl(msg.getThumbnail().getUrl());
        } catch (Exception ignored) { }
        return builder.build();
    }

    @SuppressWarnings("ConstantConditions")
    private static WebhookEmbed.EmbedField getField(MessageEmbed.Field f) {
        return new WebhookEmbed.EmbedField(f.isInline(), f.getName(), f.getValue());
    }

    @AllArgsConstructor
    @Getter
    public enum LogType {

        ERROR("Logi errorów", Ustawienia.instance.webhook.error),
        CMD("Logi komend", Ustawienia.instance.webhook.cmd),
        DEBUG("Logi debugu", Ustawienia.instance.webhook.debug),
        STATUS("Logi statusów", Ustawienia.instance.webhook.status),
        CASES("Logi Akcji", Ustawienia.instance.webhook.cases);

        private final String slownie;
        private final String url;

    }
}
