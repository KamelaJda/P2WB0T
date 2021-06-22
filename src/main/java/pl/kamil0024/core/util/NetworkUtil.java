/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.kamil0024.core.util;

import lombok.Getter;
import okhttp3.Response;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class NetworkUtil {
    private NetworkUtil() {
    }

    private static final String USER_AGENT = "P2WB0T";
    private static final String UA = "User-Agent";
    private static final String AUTH = "Authorization";

    @Getter
    public static final OkHttpClient client = new OkHttpClient();

    public static JSONResponse getJson(String url) throws IOException {
        Request req = new Request.Builder()
                .header(UA, USER_AGENT)
                .url(url)
                .build();
        Response res = client.newCall(req).execute();
        return res.body() == null ? null : new JSONResponse(res.body().string(), res.code());
    }

    public static JSONObject getJson(String url, String authorization) throws IOException {
        Request req = new Request.Builder()
                .header(UA, USER_AGENT)
                .header(AUTH, authorization)
                .url(url)
                .build();
        Response res = client.newCall(req).execute();
        return res.body() == null ? null : new JSONObject(res.body().string());
    }

    public static JSONArray getJsonArray(String url) throws IOException {
        Request req = new Request.Builder()
                .header(UA, USER_AGENT)
                .url(url)
                .build();
        Response res = client.newCall(req).execute();
        return res.body() == null ? null : new JSONArray(res.body().string());
    }

    public static JSONResponse post(String url, Object data) throws IOException {
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), GsonUtil.toJSON(data));

        Request req = new Request.Builder()
                .header(UA, USER_AGENT)
                .post(body)
                .url(url)
                .build();
        Response res = client.newCall(req).execute();
        return res.body() == null ? null : new JSONResponse(res.body().string(), res.code());
    }

    public static String encodeURIComponent(String s) {
        String result;

        try {
            result = URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            result = s;
        }

        return result;
    }

    private static Response downloadResponse(String url) throws IOException {
        Request req = new Request.Builder()
                .header(UA, USER_AGENT)
                .url(url)
                .build();
        return client.newCall(req).execute();
    }

    public static byte[] download(String url) throws IOException {
        Response res = downloadResponse(url);
        return res.body() == null ? new byte[0] : res.body().bytes();
    }

}