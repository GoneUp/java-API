import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.json.*;

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.*;
import twitter4j.conf.*;

public class crawler {
	
	
	public static String accessToken = "";
	public static List<String> history = new LinkedList<String>();
	
	public static void main(String[] args) throws Exception {
		log("Started knbot");
		while (true) {
			jodelFetcher();
			Thread.sleep(20000);
		}
	}

	public static void jodelFetcher() throws Exception {

		try {
			URL url = new URL("https://api.go-tellm.com/api/v2/posts/");
			HttpURLConnection urlConnection = (HttpURLConnection) url
					.openConnection();
			urlConnection.setRequestProperty("Authorization", "Bearer "
					+ accessToken);
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

			JSONObject jObject = new JSONObject(result);
			JSONArray jArray = jObject.getJSONArray("posts");

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

			Date now = new Date();

			for (int i = 0; i < jArray.length(); i++) {
				JSONObject oneObject = jArray.getJSONObject(i);
				// Pulling items from the array
				String message = oneObject.getString("message");
				// log(i);
				if (oneObject.has("thumbnail_url")) {
					Date d = sdf.parse(oneObject.getString("created_at"));
					if (now.getTime() - d.getTime() < 30 * 1000) {
						String imageURL = "http:" + oneObject.getString("thumbnail_url");

						URL Imageurl = new URL(imageURL);
						InputStream in = new BufferedInputStream(
								Imageurl.openStream());
						OutputStream out = new BufferedOutputStream(
								new FileOutputStream("./tmp.jpeg"));

						for (int i1; (i1 = in.read()) != -1;) {
							out.write(i1);
						}
						in.close();
						out.close();

						int maxLength = (message.length() < 139) ? message
								.length() : 139;
						message = message.substring(0, maxLength);

						picture(message, "./tmp.jpeg");
					}
				}

				else if (message.length() < 140) {
					Date d = sdf.parse(oneObject.getString("created_at"));
					//last five mins
					if (now.getTime() - d.getTime() < 5 * 60 * 1000) { 
						twitter(message);
					}
				}

			}

		} catch (Exception e) {
		}
	}

	public static void picture(String message, String pfad)
			throws TwitterException {

		if (history.contains(message))
			return;
		
		history.add(message);
			
		Twitter twitter = TwitterFactory.getSingleton();
		File file = new File(pfad);
		
		StatusUpdate status = new StatusUpdate(message);
		status.setMedia(file); // set the image to be uploaded here.
		twitter.updateStatus(status);
		
		log("Posted: " +  message);
	}

	public static void twitter(String message) {

		try {

			
			Twitter twitter = TwitterFactory.getSingleton();
			
			try {
				// get request token.
				// this will throw IllegalStateException if access token is
				// already available
				RequestToken requestToken = twitter.getOAuthRequestToken();
				log("Got request token.");
				log("Request token: " + requestToken.getToken());
				log("Request token secret: "
						+ requestToken.getTokenSecret());
				AccessToken accessToken = null;

				BufferedReader br = new BufferedReader(new InputStreamReader(
						System.in));
				while (null == accessToken) {
					String pin = br.readLine();
					try {
						if (pin.length() > 0) {
							accessToken = twitter.getOAuthAccessToken(
									requestToken, pin);
						} else {
							accessToken = twitter
									.getOAuthAccessToken(requestToken);
						}
					} catch (TwitterException te) {
						if (401 == te.getStatusCode()) {
							System.out
									.println("Unable to get the access token.");
						} else {
							te.printStackTrace();
						}
					}
				}
				log("Got access token.");
				log("Access token: " + accessToken.getToken());
				log("Access token secret: "
						+ accessToken.getTokenSecret());
			} catch (IllegalStateException ie) {
				// access token is already available, or consumer key/secret is
				// not set.
				if (!twitter.getAuthorization().isEnabled()) {
					log("OAuth consumer key/secret is not set.");
					System.exit(-1);
				}
			}
			
			if (history.contains(message))
				return;
			
			history.add(message);
			Status status = twitter.updateStatus(message);
			log("Posted: " +  message);
			
		} catch (TwitterException te) {
			te.printStackTrace();
			log("Failed to get timeline: " + te.getMessage());
			// System.exit(-1);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			log("Failed to read the system input.");
			// System.exit(-1);
		}
	}

	public static PrintWriter fileStream;
	@SuppressWarnings("deprecation")
	public static void log(String line) {
	    Date now = new Date();
		line = String.format("%d:%d %s", now.getHours(), now.getMinutes(), line);
		
		System.out.println(line);

		try {	
			if (fileStream == null) {
				fileStream = new PrintWriter(new FileOutputStream("log.txt"));		
			}
			
			fileStream.append(line + "\n");
			fileStream.flush();

		} catch (Exception ex) {

		}

	}
}
