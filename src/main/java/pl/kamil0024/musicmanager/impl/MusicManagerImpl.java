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

package pl.kamil0024.musicmanager.impl;

import lombok.Data;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.VoiceChannel;
import pl.kamil0024.musicmanager.MusicManager;

@Data
public class MusicManagerImpl implements MusicManager {

    private JDA api;
    private Guild guild;

    public MusicManagerImpl(JDA api, Guild guild) {
        this.api = api;
        this.guild = guild;
    }

    @Override
    public void connect(VoiceChannel vc) {
        if (vc == null) throw new UnsupportedOperationException("Kanał głosowy jest nullem!");
        guild.getAudioManager().openAudioConnection(vc);
    }

    @Override
    public void connect(GuildVoiceState vs) {
        if (vs == null || vs.getChannel() == null)
            throw new UnsupportedOperationException("Kanał głosowy jest nullem!");
        guild.getAudioManager().openAudioConnection(vs.getChannel());
    }

    @Override
    public void close() {
        guild.getAudioManager().closeAudioConnection();
    }

}