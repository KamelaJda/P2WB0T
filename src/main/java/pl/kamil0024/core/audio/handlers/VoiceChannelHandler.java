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

package pl.kamil0024.core.audio.handlers;

import lombok.Getter;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import pl.kamil0024.core.logger.Log;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VoiceChannelHandler implements AudioReceiveHandler {

    private final List<byte[]> bytes = new ArrayList<>();

    @Getter public final long date;
    @Getter public final String id;
    @Getter public final String userId;

    public VoiceChannelHandler(String id, String userId) {
        this.id = id;
        this.userId = userId;
        this.date = new Date().getTime();
    }

    public void save() {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            bytes.forEach(b -> {
                try {
                    output.write(b);
                } catch (IOException e) {
                    Log.error(e.getLocalizedMessage(), getClass());
                }
            });
            byte[] b = output.toByteArray();
            InputStream is = new ByteArrayInputStream(b);

            AudioFormat format = new AudioFormat(48000, 16, 2, true, true);
            AudioInputStream stream = new AudioInputStream(is, format, b.length);
            File file = new File(String.format("recordings/%s.wav", id));
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file);

            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        bytes.clear();
    }

    @Override
    public boolean canReceiveCombined() {
        return true;
    }

    @Override
    public void handleCombinedAudio(CombinedAudio combinedAudio) {
        if (combinedAudio.getUsers().isEmpty()) return;
        bytes.add(combinedAudio.getAudioData(1.0f));
    }

}
