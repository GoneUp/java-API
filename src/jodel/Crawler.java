package jodel;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import jodel.api.Jodel;
import jodel.api.JodelAuth;
import jodel.api.JodelCrypto;
import jodel.api.JodelLocation;

import org.json.*;

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.*;
import twitter4j.conf.*;

public class Crawler {

	public static Jodel client;
	public static String uid = "";
	public static String accessToken = "";
	public static List<String> history = new LinkedList<String>();

	public static JodelLocation mun = new JodelLocation("Munich", 11.5727,
			48.1410, "DE");
	public static JodelLocation kn = new JodelLocation("Konstanz", 9.171299,
			47.667856, "DE");

	public static void main(String[] args) throws Exception {
		log("Started knbot");
		try {

			if (args.length == 1) {
				// Preset accessToken
				accessToken = args[0];
			} else {
				// generate random uid, get accesstoken from server
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				md.update("test1".getBytes("UTF-8"));
				uid = String.format("%064x",
						new java.math.BigInteger(1, md.digest()));

				accessToken = new JodelAuth(uid, kn).RequestAccessToken();
			}

			client = new Jodel(accessToken);

			while (true) {
				jodelFetcher();
				Thread.sleep(20000);
			}

		} catch (Exception ex) {
			log("Main Exception: " + ex);
		}
	}

	public static void jodelFetcher() throws Exception {

		try {
			JSONObject jObj = client.getPosts(kn); // Real Location is set here!

			JSONArray jArray = null;
			if (jObj.has("posts"))
				jObj.getJSONArray("posts");
			if (jObj.has("recent"))
				jArray = jObj.getJSONArray("recent");
			if (jArray == null)
				return;

			// log(jArray.toString());

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

			Date now = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTimeZone(TimeZone.getTimeZone("GMT"));
			cal.setTime(now);

			for (int i = 0; i < jArray.length(); i++) {
				JSONObject oneObject = jArray.getJSONObject(i);
				// Pulling items from the array
				String message = oneObject.getString("message");
				Date createTime = sdf.parse(oneObject.getString("created_at"));
				boolean inRange = (now.getTime() - createTime.getTime()) < 10 * 60 * 1000;

				if (message.length() < 140 && inRange
						&& !oneObject.has("image_url")) {
					twitter(message);
				} else if (oneObject.has("image_url")) {
					String imageURL = "http:"
							+ oneObject.getString("image_url");

					URL Imageurl = new URL(imageURL);
					InputStream in = new BufferedInputStream(
							Imageurl.openStream());

					picture(message, in);
					in.close();
				}

			}

		} catch (Exception e) {
			log("Parse Exception: " + e);
		}
	}

	public static void picture(String message, InputStream bild)
			throws TwitterException {

		if (history.contains(message))
			return;

		history.add(message);

		Twitter twitter = TwitterFactory.getSingleton();

		StatusUpdate status = new StatusUpdate("Jodel Bild"); // message would
																// only contain
																// a timestamp
		status.setMedia("bild.png", bild); // set the image to be uploaded here.
		twitter.updateStatus(status);

		log("Posted: " + message);
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
				log("Request token secret: " + requestToken.getTokenSecret());
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
				log("Access token secret: " + accessToken.getTokenSecret());
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
			twitter.updateStatus(message);
			log("Posted: " + message);

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
		line = String
				.format("%d:%d %s", now.getHours(), now.getMinutes(), line);

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
