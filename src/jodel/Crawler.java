package jodel;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.*;

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

	public static long crawelTimespan;// timespan to parse in history
	public static long waittime = 5 * 60 * 1000; // 5 min, wait time for next
													// http request
	public static Jodel client;
	public static JodelAuth auth;
	public static String uid = "";
	public static String accessToken = "";
	public static Map<String, Long> history = new TreeMap<>();

	public static JodelLocation mun = new JodelLocation("Munich", 11.5727, 48.1410, "DE");
	public static JodelLocation kn = new JodelLocation("Konstanz", 9.171299, 47.667856, "DE");

	private static boolean DEBUG = false;

	public static void main(String[] args) throws Exception {
		log("Started knbot");
		postFileOutput();

		if (System.getProperty("debug") != null) {
			DEBUG = Boolean.parseBoolean(System.getProperty("debug"));
		}

		if (System.getProperty("waittime") != null) {
			// convert to minutens
			waittime = Integer.parseInt(System.getProperty("waittime")) * 60 * 1000;
		}
		
		if (System.getProperty("token") != null) {
			accessToken = System.getProperty("token");
		}
		
		crawelTimespan = waittime * 2;

		try {

			if (accessToken.isEmpty()) {
				// generate random uid, get accesstoken from server
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				md.update("test2".getBytes("UTF-8"));
				uid = String.format("%064x", new java.math.BigInteger(1, md.digest()));

				auth = new JodelAuth(uid, kn);
			}

			if (auth != null) {
				//pass auth object, enables auto auth referesh on expire (1wk usally)
				client = new Jodel(auth);
			} else {
				client = new Jodel(accessToken);
			}
			twitterLogin();

			while (true) {
				jodelFetcher();
				cleanHistory();
				Thread.sleep(waittime);
			}

		} catch (Exception ex) {
			log("Main Exception: " + ex.getMessage());
			log("Stacktrace: " + ex.getStackTrace());
		}
	}

	public static void jodelFetcher() throws Exception {

		try {
			// get posts
			JSONObject jObj = client.getPosts(kn); // Real Location is set here!

			JSONArray jArray = null;
			if (jObj.has("posts"))
				jObj.getJSONArray("posts");
			if (jObj.has("recent"))
				jArray = jObj.getJSONArray("recent");
			if (jArray == null)
				return;

			// log(jArray.toString());

			// time stuff
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
				boolean inRange = (now.getTime() - createTime.getTime()) < crawelTimespan;

				if (inRange) {
					// already posted?
					if (oneObject.has("image_url"))
						message = "http:" + oneObject.getString("image_url");

					if (history.containsKey(message))
						return;

					history.put(message, createTime.getTime());

					try {
						if (message.length() < 140 && !oneObject.has("image_url")) {
							twitter(message);
						} else if (oneObject.has("image_url")) {
							URL Imageurl = new URL(message);
							InputStream in = new BufferedInputStream(Imageurl.openStream());

							picture(message, in);
							in.close();
						}
					} catch (Exception ex) {
						log("Post Ex: " + ex);
						// Mostly twitter duplicate errors
					}
				}

			}

		} catch (Exception ex) {
			log("Fetcher Exception: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	public static void picture(String message, InputStream bild) throws TwitterException {

		Twitter twitter = TwitterFactory.getSingleton();

		StatusUpdate status = new StatusUpdate("Jodel Bild");
		status.setMedia("bild.png", bild); // set the image to be uploaded here.

		if (!DEBUG)
			twitter.updateStatus(status);

		log("Posted Pic: " + message);
	}

	public static void twitter(String message) throws TwitterException {
		Twitter twitter = TwitterFactory.getSingleton();

		if (!DEBUG)
			twitter.updateStatus(message);

		log("Posted: " + message);
	}

	private static void twitterLogin() {
		Twitter twitter = TwitterFactory.getSingleton();
		try {
			// get request token.
			// this will throw IllegalStateException if access token is
			// already available
			if (twitter.getAuthorization().isEnabled())
				return;

			RequestToken requestToken = twitter.getOAuthRequestToken();
			log("Got request token.");
			log("Request token: " + requestToken.getToken());
			log("Request token secret: " + requestToken.getTokenSecret());
			AccessToken accessToken = null;

			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			while (null == accessToken) {
				String pin = br.readLine();
				try {
					if (pin.length() > 0) {
						accessToken = twitter.getOAuthAccessToken(requestToken, pin);
					} else {
						accessToken = twitter.getOAuthAccessToken(requestToken);
					}
				} catch (TwitterException te) {
					if (401 == te.getStatusCode()) {
						System.out.println("Unable to get the access token.");
					} else {
						te.printStackTrace();
					}
				}
			}
			log("Got access token.");
			log("Access token: " + accessToken.getToken());
			log("Access token secret: " + accessToken.getTokenSecret());
		} catch (Exception ex) {
			// access token is already available, or consumer key/secret is
			// not set.
			if (!twitter.getAuthorization().isEnabled()) {
				log("OAuth consumer key/secret is not set.");
				System.exit(-1);

			}
			log("Twitter ex: " + ex);
		}
	}

	// method is used to clean the history for long running applications,
	// prevents memory leaks ^^
	private static void cleanHistory() {
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		cal.setTime(now);

		for (String key : history.keySet()) {
			// *2 just too be safe
			boolean tooOld = (history.get(key) - now.getTime()) > crawelTimespan * 2;

			if (tooOld)
				history.remove(key);
		}

	}

	public static PrintWriter fileStream;

	public static void log(String line) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));

		line = String.format("%02d:%02d %s", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), line);

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

	private static void postFileOutput() {
		try {
			File f = new File("post");
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			StringBuilder sb = new StringBuilder();

			String line = null;

			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}

			reader.close();

			JSONObject jObj = new JSONObject(sb.toString());
			JSONArray jArray = jObj.getJSONArray("children");

			PrintWriter pw = new PrintWriter(new FileOutputStream("post_out.txt"));
			pw.append(jObj.getString("created_at"));
			pw.append(" - ");
			pw.append(jObj.getString("message"));
			pw.append("\n\n");

			for (int i = 0; i < jArray.length(); i++) {
				JSONObject oneObject = jArray.getJSONObject(i);
				// Pulling items from the array
				String message = oneObject.getString("message");
				String date = oneObject.getString("created_at");

				pw.append(date);
				pw.append(" - ");
				pw.append(message);
				pw.append("\n\n");
			}

			pw.flush();
			pw.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
