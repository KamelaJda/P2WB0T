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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;

public class GsonUtil {

    private GsonUtil() {
    }

    public static final Gson GSON;

    static {
        GSON = new GsonBuilder().disableHtmlEscaping().create();
    }

    public static String toJSON(Object object) {
        return GSON.toJson(object);
    }

    public static <T> T fromJSON(String text, Class<T> object) {
        return GSON.fromJson(text, object);
    }

    public static <T> T fromJSON(String data, Type type) {
        return GSON.fromJson(data, type);
    }
}
