import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONException;
import org.json.JSONObject;


public class Jodel {
	private String accessToken;
	
	public Jodel(String token){
		accessToken = token;
	}
	
	public void putLocation(JodelLocation loc) throws Exception{
		JSONObject jsonLoc = loc.toJSONObj();
		JSONObject outer = new JSONObject();
		outer.put("location", jsonLoc);
		
		String content = outer.toString();
		String charset = StandardCharsets.UTF_8.name(); 
		System.out.println(content);
		
		URL url = new URL("https://api.go-tellm.com/api/v2/users/place");
		HttpURLConnection urlConnection = (HttpURLConnection) url
				.openConnection();
		urlConnection.setDoOutput(true); 
		urlConnection.setRequestMethod("PUT");
		urlConnection.setRequestProperty("User-Agent", "Jodel/65000 Dalvik/2.1.0 (Linux; U; Android 6.0.1; Find7 Build/MMB29M)");
		urlConnection.setRequestProperty("Content-Type", "application/json;charset=" + charset);
		urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
		
		try (OutputStream output = urlConnection.getOutputStream()) {
		    output.write(content.getBytes(charset));
		}
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				urlConnection.getInputStream(), StandardCharsets.UTF_8));

		StringBuilder sb = new StringBuilder();

		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line + "\n");
		}
		reader.close();
		crawler.log(sb.toString());
		
		
		if (urlConnection.getResponseCode() != 204){
			//fail
			crawler.log("location put failed");
		}
	}
	
	public JSONObject getPosts() throws Exception{
		URL url = new URL("https://api.go-tellm.com/api/v2/posts/");
		HttpURLConnection urlConnection = (HttpURLConnection) url
				.openConnection();
		urlConnection.setRequestProperty("User-Agent", "Jodel/65000 Dalvik/2.1.0 (Linux; U; Android 6.0.1; Find7 Build/MMB29M)");
		urlConnection.setRequestProperty("Authorization", "Bearer "+ accessToken);
		urlConnection.setRequestMethod("GET");
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				urlConnection.getInputStream(), StandardCharsets.UTF_8));

		StringBuilder sb = new StringBuilder();

		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line + "\n");
		}
		reader.close();
		String result = sb.toString();

		return new JSONObject(result);
	}
	
	public JSONObject getDistinctID() throws Exception{
		URL url = new URL("https://api.go-tellm.com/api/v2/users/distinctId ");
		HttpURLConnection urlConnection = (HttpURLConnection) url
				.openConnection();
		urlConnection.setRequestProperty("User-Agent", "Jodel/65000 Dalvik/2.1.0 (Linux; U; Android 6.0.1; Find7 Build/MMB29M)");
		urlConnection.setRequestProperty("Authorization", "Bearer "+ accessToken);
		urlConnection.setRequestMethod("GET");
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				urlConnection.getInputStream(), StandardCharsets.UTF_8));

		StringBuilder sb = new StringBuilder();

		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line + "\n");
		}
		reader.close();
		String result = sb.toString();

		return new JSONObject(result);
	}
}
