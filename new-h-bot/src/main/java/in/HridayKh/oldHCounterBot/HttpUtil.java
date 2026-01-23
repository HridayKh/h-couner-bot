package in.HridayKh.oldHCounterBot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

public class HttpUtil {
	public static long nextRequestTime = 0;

	/**
	 * Centralized method to perform HTTP requests and handle rate limits.
	 *
	 * @param method               HTTP method (GET, POST)
	 * @param url                  URL to connect to
	 * @param postBody             Request body for POST requests, null for GET
	 * @param needsAuth            Whether the request needs Authorization header
	 * @param isAccessTokenRequest Special handling for access token request (Basic
	 *                             auth)
	 * @return Response body as String, or null on error
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws InterruptedException
	 */
	public static String performHttpRequest(String method, URL url, String postBody, boolean isAccessTokenRequest)
			throws IOException, URISyntaxException, InterruptedException {

		System.out.println("\n\t---------");
		Thread.sleep(2 * 1000); // Sleep for 1 second to avoid hitting rate limits too quickly
		System.out.print("HTTP " + method + " " + url + " ");

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(method);
		conn.setDoOutput(method.equals("POST"));
		conn.setRequestProperty("User-Agent", Main.USER_AGENT);

		if (isAccessTokenRequest) {
			String auth = Main.CLIENT_ID + ":" + Main.CLIENT_SECRET;
			String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
			conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
		} else {
			conn.setRequestProperty("Authorization", "Bearer " + Main.TOKEN);
		}

		if (method.equals("POST") && postBody != null && !postBody.isEmpty()) {
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			try (OutputStream os = conn.getOutputStream()) {
				os.write(postBody.getBytes(StandardCharsets.UTF_8));
			}
		}

		int responseCode = conn.getResponseCode();
		String responseMessage = conn.getResponseMessage();

		String remaining = conn.getHeaderField("X-Ratelimit-Remaining");
		String reset = conn.getHeaderField("X-Ratelimit-Reset");
		String used = conn.getHeaderField("X-Ratelimit-Used");

		InputStream responseStream = responseCode >= 200 && responseCode < 300 ? conn.getInputStream()
				: conn.getErrorStream();

		StringBuilder responseBuilder = new StringBuilder();
		if (responseStream != null) {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(responseStream))) {
				String line;
				while ((line = in.readLine()) != null) {
					responseBuilder.append(line);
				}
			}
		}
		String responseBody = responseBuilder.toString();

		if (conn != null)
			conn.disconnect();

		if (remaining != null && reset != null) {
			rateLimit(remaining, reset, used, responseCode);
		}

		System.out.println(responseMessage + "(" + responseCode + ") " + responseBody.length());
		System.out.println(responseBody.length() > 100 ? responseBody.substring(0, 100) + "..." : responseBody);
//		System.out.println(responseBody.replaceAll(" ", ""));
		System.out.println("Remaining=" + remaining + ", Reset=" + reset + ", Used=" + used);

		if (responseCode >= 200 && responseCode < 300) {
			return responseBody;
		} else {
			System.err.println("\n\n\nHTTP request failed with code: " + responseCode + " - " + responseMessage);
			System.err.println(responseBody);
			return null;
		}
	}

	private static void rateLimit(String remaining, String reset, String used, int responseCode)
			throws InterruptedException {
		try {

			int remainingRequests = (int) Double.parseDouble(remaining);
			long resetTimeSeconds = (long) Double.parseDouble(reset);

			if (remainingRequests < 5 || responseCode == 429) {
				long sleepFor = (resetTimeSeconds * 1000) + 2000;
				nextRequestTime = System.currentTimeMillis() + sleepFor;

				System.out.println("Rate limit warning/hit! Remaining: " + remainingRequests + ", Used: " + used
						+ ", Reset in: " + resetTimeSeconds + "s.");

				System.out.println("Pausing all API requests until: " + LocalDateTime
						.ofInstant(java.time.Instant.ofEpochMilli(nextRequestTime), java.time.ZoneId.systemDefault()));

				System.err.println("HTTP 429 Too Many Requests. Forced sleep for " + (sleepFor / 1000) + "s.");
				Thread.sleep(sleepFor);
			}

		} catch (NumberFormatException e) {
			System.err.println("Error parsing rate limit headers: " + e.getMessage());
		}

	}

}
