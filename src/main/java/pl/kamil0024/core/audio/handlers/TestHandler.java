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

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import pl.kamil0024.core.logger.Log;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestHandler implements AudioSendHandler, AudioReceiveHandler {

    private final Queue<byte[]> queue = new ConcurrentLinkedQueue<>();
    private final List<byte[]> bytes = new ArrayList<>();

    public TestHandler() throws FileNotFoundException {
        try {
            Runnable task = () -> {
                try {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    bytes.forEach(b -> {
                        try {
                            output.write(b);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    byte[] b = output.toByteArray();
                    InputStream is = new ByteArrayInputStream(b);

                    AudioFormat format = new AudioFormat(48000, 16, 2, true, true);
                    AudioInputStream stream = new AudioInputStream(is, format, b.length);
                    File file = new File("test.wav");
                    AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file);

                    is.close();
                    Log.debug("Plik zapisany");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
            ses.schedule(task, 25, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean canReceiveCombined() {
        return queue.size() < 30;
    }

    @Override
    public void handleCombinedAudio(CombinedAudio combinedAudio) {
        if (combinedAudio.getUsers().isEmpty()) {
            return;
        }
        byte[] data = combinedAudio.getAudioData(1.0f);
        bytes.add(data);
        queue.add(data);
    }

    @Override
    public boolean canProvide() {
        return !queue.isEmpty();
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        byte[] data = queue.poll();
        return data == null ? null : ByteBuffer.wrap(data);
    }

    @Override
    public boolean isOpus() {
        return false;
    }

}
