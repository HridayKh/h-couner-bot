package main;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

public class TokenAuth {
	/**
	 * Obtains an OAuth2 access token from Reddit using username/password grant
	 * type.
	 *
	 * @return The access token string, or null if an error occurs.
	 */
	public static String[] getAccessToken() {
		try {
			URL url = new URI("https://oauth.reddit.com/api/v1/access_token").toURL();
			String body = "grant_type=password&username=" + URLEncoder.encode(Main.REDDIT_USERNAME, "UTF-8")
					+ "&password=";
			try {
				body += URLEncoder.encode(Main.REDDIT_PASSWORD, StandardCharsets.UTF_8.toString());
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("UTF-8 encoding not supported for password, which is unexpected.", e);
			}

			String responseBody = HttpUtil.performHttpRequest("POST", url, body, true);
			JSONObject jsonResponse = new JSONObject(responseBody);

			if (responseBody != null) {
				return new String[] { jsonResponse.getString("access_token"),
						String.valueOf(jsonResponse.getInt("expires_in")) };
			}
		} catch (Exception e) {
			System.err.println("An unexpected error occurred in getAccessToken: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

}
