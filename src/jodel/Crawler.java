package jodel;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

	public static long crawelTimespan = 0;// timespan to parse in history
	public static long waittime = 5 * 60 * 1000; // 5 min, wait time for next
													// http request
	public static Jodel client;
	public static JodelAuth auth;
	public static String uid = "";
	public static String accessToken = "";
	public static Map<String, Long> history = new TreeMap<>();

	public static JodelLocation mun = new JodelLocation("DE", "Munich", 48.1410, 11.5727);
	public static JodelLocation kn = new JodelLocation("DE", "Konstanz", 47.667856, 9.171299);

	private static boolean DEBUG = false;

	public static void main(String[] args) throws Exception {
		log("Started knbot");

		if (System.getProperty("debug") != null) {
			DEBUG = Boolean.parseBoolean(System.getProperty("debug"));
		}

		if (System.getProperty("waittime") != null) {
			// convert to minutens
			waittime = Integer.parseInt(System.getProperty("waittime")) * 60 * 1000;
		}

		if (System.getProperty("crawelTimespan") != null) {
			crawelTimespan = Integer.parseInt(System.getProperty("crawelTimespan")) * 60 * 1000;
		} else {
			crawelTimespan = waittime * 2;
		}

		log(String.format("Arg - Debug %s, crawelTime %s, waitTime %s", DEBUG, crawelTimespan, waittime));
		
		if (System.getProperty("token") != null) {
			accessToken = System.getProperty("token");
		}

		if (System.getProperty("location") != null) {
			// LAT;LNG
			// 47.667856;9.171299
			try {
				String[] split = System.getProperty("location").split(":");
				double lat = Double.parseDouble(split[0]);
				double lng = Double.parseDouble(split[1]);

				if (split.length == 2) {
					kn = new JodelLocation("DE", "Stadt", lat, lng);
				}
			} catch (Exception ex) {
				log("Location format error. Needs to the a double and in the format 'LAT;LNG'");
			}
		}

		try {

			if (accessToken.isEmpty()) {
				// generate random uid, get accesstoken from server
				String toEncrypt = "test2";
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				md.update(toEncrypt.getBytes("UTF-8"));
				uid = String.format("%064x", new java.math.BigInteger(1, md.digest()));

				auth = new JodelAuth(uid, kn);
			}

			if (auth != null) {
				// pass auth object, enables auto auth referesh on expire (1wk
				// usally)
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
			List<JSONObject> jObjs = client.getPosts(kn); // Real Location is set here!
			JSONObject jObj = jObjs.get(0);
			if (DEBUG) log(jObj.toString());
			
			JSONArray jArray = null;
			if (jObj.has("posts"))
				jArray = jObj.getJSONArray("posts");
			if (jObj.has("recent"))
				jArray = jObj.getJSONArray("recent");
			if (jArray == null)
				return;

			// log(jArray.toString());

			// time stuff
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

			Calendar cal = Calendar.getInstance();
			cal.setTimeZone(TimeZone.getTimeZone("GMT"));


			for (int i = jArray.length() - 1; i > 0; --i) {
				JSONObject oneObject = jArray.getJSONObject(i);
				// Pulling items from the array
				String id = oneObject.getString("post_id");
				String message = oneObject.getString("message");
				boolean isTeam = oneObject.getJSONObject("location").getString("name").equals("Jodel Team");
				if (isTeam) {
					if (DEBUG) log("TEAM SKIP " + message);
					continue;
				}
				
				
				Date createTime = sdf.parse(oneObject.getString("created_at"));
				boolean inRange = (cal.getTimeInMillis() - createTime.getTime()) < crawelTimespan;

				if (inRange) {

					// already posted?
					if (oneObject.has("image_url"))
						message = "http:" + oneObject.getString("image_url");

					if (history.containsKey(id)) {
						continue;
					}

					history.put(id, createTime.getTime());

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
}
