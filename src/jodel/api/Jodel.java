package jodel.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import jodel.Crawler;

import org.json.JSONException;
import org.json.JSONObject;

public class Jodel {
	public static final String API_URL = "https://api.go-tellm.com/api/v2";
	private String accessToken;
	private JodelAuth auth;

	public Jodel(String token) {
		accessToken = token;
	}

	public Jodel(JodelAuth auth) {
		this.auth = auth;
	}

	private HttpURLConnection createCon(URL url, String method) throws Exception {
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		urlConnection.setRequestProperty("User-Agent",
				"Jodel/4.3.7 Dalvik/2.1.0 (Linux; U; Android 6.0.1; Find7 Build/MMB29M)");
		urlConnection.setRequestMethod(method);
		if (auth != null) {
			urlConnection.setRequestProperty("Authorization", "Bearer " + auth.getToken());
		} else {
			urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
		}
		
		// x headers
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		urlConnection.setRequestProperty("X-Timestamp", sdf.format(new Date()));
		urlConnection.setRequestProperty("X-Client-Type", "android_4.3.7");
		urlConnection.setRequestProperty("X-Api-Version", "0.1");
		urlConnection.setRequestProperty("X-Authorization", "HMAC CDCAC434FE4363C3ADFB696ECDBECC9BCB7B421E");
		return urlConnection;
	}

	private String parseResponse(HttpURLConnection urlConnection) throws IOException {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8));

		StringBuilder sb = new StringBuilder();

		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line + "\n");
		}
		reader.close();

		return sb.toString();
	}

	public void putLocation(JodelLocation loc) throws Exception {
		JSONObject jsonLoc = loc.toJSONObj();
		JSONObject outer = new JSONObject();
		outer.put("location", jsonLoc);

		String content = outer.toString();
		String charset = StandardCharsets.UTF_8.name();
		System.out.println(content);

		URL url = new URL(API_URL + "/users/place");
		HttpURLConnection urlConnection = createCon(url, "PUT");
		urlConnection.setDoOutput(true);

		try (OutputStream output = urlConnection.getOutputStream()) {
			output.write(content.getBytes(charset));
		}

		String result = parseResponse(urlConnection);
		Crawler.log(result);

		if (urlConnection.getResponseCode() != 204) {
			// fail
			Crawler.log("location put failed");
		}
	}

	public JSONObject getPosts(JodelLocation loc) throws Exception {
		// GET /api/v2/posts/location/combo?lat=47.6750029&lng=9.1720287
		Formatter formatter = new Formatter(Locale.US);
		URL url = new URL(
				formatter.format(API_URL + "/posts/location/combo?lat=%f&lng=%f", loc.lat, loc.lng)
						.toString());
		HttpURLConnection urlConnection = createCon(url, "GET");
		Crawler.log(urlConnection.getRequestProperties().toString());
		
		// get content
		String result = parseResponse(urlConnection);
		return new JSONObject(result);
	}

	public JSONObject getDistinctID() throws Exception {
		URL url = new URL(API_URL + "/distinctID");
		HttpURLConnection urlConnection = createCon(url, "GET");

		String result = parseResponse(urlConnection);
		Crawler.log(result);

		return new JSONObject(result);
	}

	public JSONObject getPostInfo(String id) throws Exception {
		URL url = new URL(API_URL + "/posts/" + id + "/");
		HttpURLConnection urlConnection = createCon(url, "GET");
		urlConnection.setRequestProperty("X-Authorization", "HMAC 0F7D7FA3010E92E3F68774E8CF126FA4C5851DFE");

		String result = parseResponse(urlConnection);
		Crawler.log(result);

		return new JSONObject(result);
	}

	public void putToken() throws Exception {
		JSONObject outer = new JSONObject();
		outer.put("client_id", JodelAuth.clientid);
		outer.put("push_token",
				"APA91bFwFCBHXPdCCGoPQGSBj9wkdelRAtQgkRPmao5Shyi10s_BoaajReKG4Km-UHc8jW8spELpKr-jgbdxHY25k_taFf1VM3d7qPlcphavZ5PjPUBknozcLFq8NDKX4xDayJZh5pVD");

		String content = outer.toString();
		String charset = StandardCharsets.UTF_8.name();
		System.out.println(content);

		URL url = new URL(API_URL + "/users/pushToken/");
		HttpURLConnection urlConnection = createCon(url, "PUT");
		urlConnection.setDoOutput(true);

		try (OutputStream output = urlConnection.getOutputStream()) {
			output.write(content.getBytes(charset));
		}

		String result = parseResponse(urlConnection);
		Crawler.log(result);
		
		if (urlConnection.getResponseCode() != 204) {
			// fail
			Crawler.log("location put failed");
		}
	}
}
