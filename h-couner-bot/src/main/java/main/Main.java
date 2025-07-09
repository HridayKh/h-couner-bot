package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class Main {

	public static String USER_AGENT = "script_in_java:h-counter-bot:v1.1 (by u/PROMAN8625)";
	public static final String CLIENT_ID = System.getenv("h_bot_id");
	public static final String CLIENT_SECRET = System.getenv("h_bot_secret");
	public static final String REDDIT_USERNAME = System.getenv("h_bot_username");
	public static final String REDDIT_PASSWORD = System.getenv("h_bot_pass");

	public static String TOKEN = "";

	private static final String PROCESSED_IDS_FILE = System.getenv("h_bot_file");
	public static Set<String> processedMessageFullnames = new HashSet<>();

	private static LocalDateTime token_exp = null;

	public static void main(String[] args) {
		new Main();
	}

	public Main() {
		loadProcessedMessageFullnames();
		while (true) {
			try {
				long currentTime = System.currentTimeMillis();
				if (currentTime < HttpUtil.nextRequestTime) {
					long sleepDuration = HttpUtil.nextRequestTime - currentTime;
					System.out
							.println("Global API cooldown active. Sleeping for " + sleepDuration / 1000 + " seconds.");
//					Thread.sleep(sleepDuration);
				}

				update_loop();
//				Thread.sleep(5000);
//			} catch (InterruptedException e) {
//				Thread.currentThread().interrupt();
//				System.err.println("Bot interrupted: " + e.getMessage());
//				break;
			} catch (Exception e) {
				System.err.println("An unexpected error occurred in main loop: " + e.getMessage());
				e.printStackTrace();
//				try {
//					Thread.sleep(30000); // sleep for 30s during unhandled exceptions
//				} catch (InterruptedException ie) {
//					Thread.currentThread().interrupt();
//				}
			}
		}
	}

	public void update_loop() {
		LocalDateTime rn = LocalDateTime.now();

		if (TOKEN == null || token_exp == null || rn.isAfter(token_exp.minusMinutes(1))) {
			System.out.println("Access token is missing or expired, attempting to refresh...");
			String[] newTokenArr = getAccessToken();
			String newToken = newTokenArr != null ? newTokenArr[0] : null;
			int expiresIn = newTokenArr != null ? Integer.parseInt(newTokenArr[1]) : null;

			if (newToken != null) {
				TOKEN = newToken;
				token_exp = rn.plusSeconds(expiresIn);
				System.out.println("Access token refreshed successfully. Expires at: " + token_exp);
			} else {
				System.err.println("Failed to obtain access token. Cannot proceed with update loop.");
				return;
			}
		}

		List<String> fullnamesToMarkAsRead = new ArrayList<>();

		List<String[]> unreadMentions = ReadComments.getUnreadMessagesAndFilterMentions(fullnamesToMarkAsRead);

		for (String[] mention : unreadMentions) {
			if (mention.length < 4) {
				System.err.println("Skipping malformed mention array: " + String.join(",", mention));
				continue;
			}

			String id = mention[0]; // The base ID (e.g., "n1lqt5s")
			String author = mention[1]; // The author of the mention
			String type = mention[2]; // The user being queried (e.g., "PROMAN8625")
			String fullname = mention[3]; // The full ID (e.g., "t1_n1lqt5s")

			System.out.println("Processing Mention ID: " + id + ", Author: " + author + ", Target User: " + type);

			try {
				// Fetch comments for the target user (type)
				String[] comments = getUserComments(type);
				long[] inf = getCommentInfo(comments);
				long totalChars = inf[0] + 1;
				long totalH = inf[1];
				long totalComments = comments.length;
				double hScoreRaw = totalH / (totalChars - totalH);
				String hScore = String.format("%.4f", hScoreRaw * 100);
				String rating = "";
				if (hScoreRaw < 1) {
					rating = "Fuck You. Seriously, where's the h? Did you even try?"; // Added a bit more sass
				} else if (hScoreRaw < 2) { // This still catches 0 < hScoreRaw < 1
					rating = "Is that even an h? I can barely see h! Or do my eyes deceive me."; // More dramatic
				} else if (hScoreRaw < 3) {
					rating = "Barely an h enthusiast. Get those fingers moving, pal."; // More direct, casual
				} else if (hScoreRaw < 6) {
					rating = "Solid h game, not gonna lie. Pretty average, though."; // More conversational
				} else if (hScoreRaw < 10) {
					rating = "Above average h usage! You're clearly a person of culture."; // Casual compliment
				} else if (hScoreRaw < 20) {
					rating = "Whoa, that's a lot of hs! Are you sexually attracted to it?"; // Playful surprise
				} else if (hScoreRaw < 50) {
					rating = "Ah yes, an h enthusiast, HhHhHhHh. Impressive."; // More vivid imagery
				} else if (hScoreRaw < 100) {
					rating = "The h whisperer! You speak fluent h. We are not worthy.";
				} else if (hScoreRaw < 200) {
					rating = "Legendary h status! You're almost an h-bot"; // More epic
				} else if (totalH / totalChars >= 99) {
					rating = "exprected from an h bot, you got more h than comments.";
				} else {
					rating = "An h demigod! Your h count is off the charts, but still human....... probably.";
				}

				String result = "Hey u/" + author + "! You wanted to know about the hScore of u/" + type + "?\n\n"
						+ "Well, here's the lowdown:\n" + "u/" + type + " has dropped " + totalComments
						+ " comments, flexing a grand total of " + totalChars + " characters.\n"
						+ "Within those, I meticulously counted a whopping `" + totalH + "` *h* or *H* characters!\n\n"
						+ "That brings us to the moment of truth: the legendary H-Score (that's 'h's per every non-'h' character, for the uninitiated) is a solid ***"
						+ hScore + "***, which is a rating of " + rating
						+ "\n\nThis message was brought to you by the H-Counter Bot, report error or issues to the mods or r/hcounterbot";
				boolean success = replyToComment(id, result);
				if (success) {
					System.out.println("Successfully sent reply to mention ID: " + id);
					// Add to list for batch marking as read
					fullnamesToMarkAsRead.add(fullname);
					processedMessageFullnames.add(fullname); // Add to our set immediately
				} else {
					System.err.println("Failed to send reply to mention ID: " + id);
					// If reply failed, still mark as read to avoid reprocessing, but log it.
					fullnamesToMarkAsRead.add(fullname);
					processedMessageFullnames.add(fullname);
				}
			} catch (Exception e) {
				System.err.println("An error occurred during processing mention ID " + id + ": " + e.getMessage());
				e.printStackTrace();
				// Add to list for batch marking as read even on error to prevent reprocessing
				fullnamesToMarkAsRead.add(fullname);
				processedMessageFullnames.add(fullname);
			}
		}

		// Mark all collected messages as read in a single batch request
		if (!fullnamesToMarkAsRead.isEmpty()) {
			System.out.println("Attempting to batch mark " + fullnamesToMarkAsRead.size() + " messages as read.");
			boolean batchMarked = markMessagesAsReadBatch(fullnamesToMarkAsRead);
			if (batchMarked) {
				System.out.println("Successfully batch marked messages as read.");
				saveProcessedMessageFullnames(); // Persist the updated set after successful batch marking
			} else {
				System.err.println(
						"Failed to batch mark messages as read. Processed IDs might not be persisted correctly.");
			}
		}
	}

	/**
	 * Marks multiple messages (comments, DMs, etc.) as read on Reddit in a single
	 * batch request.
	 *
	 * @param fullnames A list of full IDs of the messages to mark as read (e.g.,
	 *                  "t1_abcdef", "t4_123456").
	 * @return true if the messages were successfully marked as read, false
	 *         otherwise.
	 */
	public boolean markMessagesAsReadBatch(List<String> fullnames) {
		if (fullnames == null || fullnames.isEmpty()) {
			System.out.println("No messages to batch mark as read.");
			return true; // Nothing to mark, consider it successful
		}

		try {
			URL url = new URI("https://oauth.reddit.com/api/read_message").toURL();
			// Join fullnames with comma and URL-encode each one
			String idsParam = fullnames.stream().map(s -> {
				try {
					return URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
				} catch (UnsupportedEncodingException e) {
					// UTF-8 is guaranteed to be supported, so this should ideally never happen.
					// Re-throw as a RuntimeException to satisfy the Stream API's functional
					// interface.
					throw new RuntimeException("UTF-8 encoding not supported, which is unexpected.", e);
				}
			}).collect(Collectors.joining(","));

			String postBody = "id=" + idsParam;

			String responseBody = HttpUtil.performHttpRequest("POST", url, postBody, true, false);

			return responseBody != null; // If responseBody is not null, request was successful (2xx)
		} catch (Exception e) {
			System.err.println("Error batch marking messages as read: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Replies to a Reddit comment.
	 *
	 * @param thingId   The ID of the comment to reply to (e.g., "n1lqt5s").
	 * @param replyText The text of the reply.
	 * @return true if the reply was successfully posted, false otherwise.
	 */
	public boolean replyToComment(String thingId, String replyText) {
		try {
			URL url = new URI("https://oauth.reddit.com/api/comment").toURL();
			String fullThingId = "t1_" + thingId; // Reddit API expects the full ID (e.g., "t1_n1lqt5s")

			String postBody = "api_type=json" + "&text="
					+ URLEncoder.encode(replyText, StandardCharsets.UTF_8.toString()) + "&thing_id="
					+ URLEncoder.encode(fullThingId, StandardCharsets.UTF_8.toString());

			String responseBody = HttpUtil.performHttpRequest("POST", url, postBody, true, false);

			if (responseBody != null) {
				JSONObject jsonResponse = new JSONObject(responseBody);
				JSONObject json = jsonResponse.optJSONObject("json");
				if (json != null) {
					if (json.has("errors") && json.getJSONArray("errors").length() > 0) {
						System.err.println("API Errors during reply: " + json.getJSONArray("errors").toString());
						return false;
					} else if (json.has("data") && json.optJSONObject("data").has("things")) {
						return true;
					}
				}
				return true;
			}
			return false;
		} catch (Exception e) {
			System.err.println("An error occurred during replyToComment: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Calculates total characters and total h characters in an array of comments.
	 *
	 * @param comments An array of comment strings.
	 * @return A long array where index 0 is total characters and index 1 is total h
	 *         characters.
	 */
	private long[] getCommentInfo(String[] comments) {
		long totalChars = 0;
		long totalH = 0;
		for (String comment : comments) {
			totalChars += comment.length();
			totalH += comment.length() - comment.toLowerCase().replace("h", "").length();
		}
		return new long[] { totalChars, totalH };
	}

	/**
	 * Fetches comments made by a specific Reddit user.
	 *
	 * @param author The username of the author whose comments to fetch.
	 * @return An array of comment bodies as strings.
	 */
	public String[] getUserComments(String author) {
		List<String> allComments = new ArrayList<>();
		String after = null;
		boolean hasMoreComments = true;
		int limit = 100;

		while (hasMoreComments) {
			StringBuilder urlBuilder = new StringBuilder("https://www.reddit.com/user/").append(author)
					.append("/comments/.json?limit=").append(limit);

			if (after != null) {
				urlBuilder.append("&after=").append(after);
			}

			String url = urlBuilder.toString();

			try {
				URL apiUrl = new URI(url).toURL();
				String responseBody = HttpUtil.performHttpRequest("GET", apiUrl, null, false, false);
				System.out.println("\n\n" + apiUrl + "\n" + responseBody + "\n\n");
				if (responseBody != null) {
					JSONObject jsonResponse = new JSONObject(responseBody);
					JSONObject data = jsonResponse.getJSONObject("data");
					JSONArray commentsArray = data.getJSONArray("children");

					if (commentsArray.length() <= 100) {
						hasMoreComments = false;
						break;
					}

					for (int i = 0; i < commentsArray.length(); i++) {
						allComments.add(commentsArray.getJSONObject(i).getJSONObject("data").getString("body"));
					}

					// Get the 'after' value for the next request
					after = data.optString("after", null); // Use optString to handle null gracefully
					if (after == null || after.isEmpty()) {
						hasMoreComments = false; // No more pages
					}

				} else {
					System.err.println("Empty response body for user " + author);
					hasMoreComments = false; // Stop if response is null
				}
			} catch (Exception e) {
				System.err.println("Error fetching user comments for " + author + ": " + e.getMessage());
				e.printStackTrace();
				hasMoreComments = false; // Stop on error
			}
		}
		return allComments.toArray(new String[0]);
	}

	/**
	 * Obtains an OAuth2 access token from Reddit using username/password grant
	 * type.
	 *
	 * @return The access token string, or null if an error occurs.
	 */
	private String[] getAccessToken() {
		try {
			URL url = new URI("https://www.reddit.com/api/v1/access_token").toURL();
			String body = "grant_type=password&username=" + URLEncoder.encode(REDDIT_USERNAME, "UTF-8") + "&password=";
			try {
				body += URLEncoder.encode(REDDIT_PASSWORD, StandardCharsets.UTF_8.toString());
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("UTF-8 encoding not supported for password, which is unexpected.", e);
			}

			String responseBody = HttpUtil.performHttpRequest("POST", url, body, true, true);
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

	// --- Helper methods for loading and saving processed IDs ---

	/**
	 * Loads previously processed message fullnames from a file into the HashSet.
	 */
	private void loadProcessedMessageFullnames() {
		if (PROCESSED_IDS_FILE == null || PROCESSED_IDS_FILE.isEmpty()) {
			System.err.println(
					"PROCESSED_IDS_FILE environment variable not set. Processed messages will not be persisted.");
			return;
		}

		File file = new File(PROCESSED_IDS_FILE);
		try {
			if (!file.exists()) {
				System.out.println("Processed IDs file not found. Attempting to create: " + PROCESSED_IDS_FILE);
				if (file.getParentFile() != null && !file.getParentFile().exists()) {
					file.getParentFile().mkdirs(); // Create parent directories
				}
				if (file.createNewFile()) {
					System.out.println("Successfully created new processed IDs file: " + PROCESSED_IDS_FILE);
				} else {
					System.err.println("Failed to create new processed IDs file: " + PROCESSED_IDS_FILE);
					return;
				}
			}

			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = reader.readLine()) != null) {
					processedMessageFullnames.add(line.trim());
				}
				System.out.println("Loaded " + processedMessageFullnames.size() + " processed message fullnames from "
						+ PROCESSED_IDS_FILE);
			}
		} catch (IOException e) {
			System.err.println("Could not load processed message fullnames from file " + PROCESSED_IDS_FILE + ": "
					+ e.getMessage());
		}
	}

	private void saveProcessedMessageFullnames() {
		if (PROCESSED_IDS_FILE == null || PROCESSED_IDS_FILE.isEmpty()) {
			System.err.println("PROCESSED_IDS_FILE environment variable not set. Cannot save processed messages.");
			return;
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(PROCESSED_IDS_FILE))) {
			for (String fullname : processedMessageFullnames) {
				writer.write(fullname);
				writer.newLine();
			}
			System.out.println("Saved " + processedMessageFullnames.size() + " processed message fullnames to "
					+ PROCESSED_IDS_FILE);
		} catch (IOException e) {
			System.err.println(
					"Could not save processed message fullnames to file " + PROCESSED_IDS_FILE + ": " + e.getMessage());
			e.printStackTrace();
		}

	}

}
