package jodel.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import org.json.JSONObject;

import jodel.Crawler;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Jodel {
	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	public static final String API_URL = "https://api.go-tellm.com/api/v2";

	private String accessToken;
	private JodelAuth auth;
	private OkHttpClient client = new OkHttpClient();

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

	private Request createCon(String url, String method, String body) throws Exception {
		Request.Builder builder = new Request.Builder().url(url).addHeader("User-Agent",
				"Jodel/4.31.0 Dalvik/2.1.0 (Linux; U; Android 6.0.1; Find7 Build/MMB29M)");

		if (method.equals("GET")) {
			builder.get();
		} else if (method.equals("PUT")) {
			builder.put(RequestBody.create(JSON, body));
		} else if (method.equals("POST")) {
			builder.post(RequestBody.create(JSON, body));
		}

		if (auth != null) {
			builder.addHeader("Authorization", "Bearer " + auth.getToken());
		} else {
			builder.addHeader("Authorization", "Bearer " + accessToken);
		}

		// x headers
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		builder.addHeader("X-Timestamp", sdf.format(new Date()));
		builder.addHeader("X-Client-Type", "android_4.31.0");
		builder.addHeader("X-Api-Version", "0.2");
		builder.addHeader("X-Authorization", "HMAC CDCAC434FE4363C3ADFB696ECDBECC9BCB7B421E");

		return builder.build();
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
		urlConnection.disconnect();

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

	public List<JSONObject> getPosts(JodelLocation loc) throws Exception {
		List<JSONObject> results = new ArrayList<>();
		
		// GET /api/v2/posts/location/combo?lat=47.6750029&lng=9.1720287
		///posts/location?lat=%f&lng=%f"
		Formatter formatter = new Formatter(Locale.US);
		String url = formatter.format(API_URL + "/posts/location/combo?lat=%f&lng=%f", loc.lat, loc.lng).toString();

		// first call
		Request request = createCon(url.toString(), "GET", "");
		Response response = client.newCall(request).execute();

		String result = response.body().string();
		results.add(new JSONObject(result));
		
		/*
		for (int i = 0; i < 3; ++i){
			String iterateUrl = url + String.format("&after=%d&limit=1000");
			request = createCon(url.toString(), "GET", "");
			response = client.newCall(request).execute();

			result = response.body().string();
			results.add(new JSONObject(result));
		}
		*/
		
		return results;
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
