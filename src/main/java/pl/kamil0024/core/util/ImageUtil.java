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

import net.dv8tion.jda.api.entities.Message;
import pl.kamil0024.commands.dews.ShellCommand;
import pl.kamil0024.core.logger.Log;

import java.io.File;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("unused")
public class ImageUtil {

    private final static Random RANDOM = new Random();

    public static CompletableFuture<String> readFile(Message.Attachment ath) {
        String fileName = RANDOM.nextInt(Integer.MAX_VALUE) + "." + ath.getFileExtension();

        try {
            String outFile = "cache/" + fileName;
            File f = ath.downloadToFile(outFile).get();
            if (f.exists()) {
                String ls = f.getAbsolutePath();
                ShellCommand.shell(String.format("tesseract %s %s -l pol", ls, outFile));

                String ret = ShellCommand.shell("cat " + outFile + ".txt");
                new Thread(() -> {
                    ShellCommand.shell("rm " + outFile);
                    ShellCommand.shell("rm " + outFile + ".txt");
                }).start();

                return CompletableFuture.completedFuture(ret);

            } else Log.error("Nie Istnieje!");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return CompletableFuture.completedFuture(null);
    }

}
