package jodel.api;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import jodel.Crawler;
import jodel.RandomString;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class JodelAuth {
	//according to the apk source the clientid is hardcoded. user auth is only based on the uid wich accepts anything
	
	//POST /api/v2/users/ HTTP/1.1	
	//{"client_id":"81e8a76e-1e02-4d17-9ba0-8a7020261b26","device_uid":"xxx",
	//"location":{"loc_accuracy":30.0,"city":"Konstanz","loc_coordinates":{"lat":47.674943,"lng":9},"country":"DE"}}
	
	//{"access_token":"xx","refresh_token":"c284d121-2ce1-4358-955e-95ae45a2c66c","token_type":"bearer","expires_in":604800,"expiration_date":1453677510,"distinct_id":"5695fd1f8a22f0d304bbabc0","returning":true}

	public static final String clientid = "81e8a76e-1e02-4d17-9ba0-8a7020261b26";
	
	private final String uid;
	private String accessToken = "";
	private long expireDate;

	private JodelLocation loc;
	
	public JodelAuth(){
		//client id 36 
		//uid 64
		
		uid = new RandomString(64).nextString();
		loc = new JodelLocation("DE", "Konstanz", 47.667856, 9.171299);
	}

	public JodelAuth(String uid, JodelLocation loc) {
		this.uid = uid;
		this.loc = loc;
	}
	
	public String RequestAccessToken() throws IOException, JSONException{
		//content
		JSONObject jsonLoc = loc.toJSONObj();
	
		JSONObject jsonReq = new JSONObject();
		jsonReq.put("client_id", clientid);
		jsonReq.put("device_uid", uid);		
		jsonReq.put("location", jsonLoc);

		String content = jsonReq.toString();
		String charset = StandardCharsets.UTF_8.name(); 
		Crawler.log(content);
		
		//headers
		URL url = new URL(Jodel.API_URL + "/users/");
		HttpURLConnection urlConnection = (HttpURLConnection) url
				.openConnection();
		urlConnection.setDoOutput(true); 
		urlConnection.setRequestMethod("POST");
		urlConnection.setRequestProperty("User-Agent", "Jodel/4.3.7 Dalvik/2.1.0 (Linux; U; Android 6.0.1; Find7 Build/MMB29M)");
		urlConnection.setRequestProperty("Content-Type", "application/json;charset=" + charset);
		
		//x headers
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		urlConnection.setRequestProperty("X-Timestamp", sdf.format(new Date()));
		urlConnection.setRequestProperty("X-Client-Type", "android_4.3.7");
		urlConnection.setRequestProperty("X-Api-Version", "0.1");
		urlConnection.setRequestProperty("X-Authorization", "HMAC CDCAC434FE4363C3ADFB696ECDBECC9BCB7B421E");

		try (OutputStream output = urlConnection.getOutputStream()) {
		    output.write(content.getBytes(charset));
		}
			
		//response
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				urlConnection.getInputStream(), StandardCharsets.UTF_8));

		StringBuilder sb = new StringBuilder();

		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line + "\n");
		}
		reader.close();
		String result = sb.toString();
		System.out.println(result);
		
		//parse response
		JSONObject jObject = new JSONObject(result);		
		expireDate = jObject.getInt("expiration_date") + jObject.getInt("expires_in");
		accessToken = jObject.getString("access_token");
		Crawler.log("Refreshed access token! " + result);
		return jObject.getString("access_token");
		
	}
	
	public String getToken() throws Exception{
		if (accessToken.isEmpty() || isExpired())
			RequestAccessToken();
		return 	accessToken;
	}
	
	
	public boolean isExpired(){	
		return (System.currentTimeMillis() / 1000) > expireDate;
	}
	

}
