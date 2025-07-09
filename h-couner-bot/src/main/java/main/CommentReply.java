package main;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;

public class CommentReply {

	public static String determineResult(long[] inf, int totalComments, String author, String targetUser) {
		double totalChars = (double) inf[0] + 1.0;
		double totalH = (double) inf[1];
		double hScoreRaw = (totalH / (totalChars - totalH)) * 100;
		// tc: 48563, h: 1786, tm: 954
		String hScore = String.format("%.4f", hScoreRaw);
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
		} else if (totalH >= totalComments-10) {
			rating = "exprected from an h bot, you got more h than comments.";
		} else {
			rating = "An h demigod! Your h count is off the charts, but still human....... probably.";
		}

		return "Hey u/" + author + "! You wanted to know about the hScore of u/" + targetUser + "?\n\n"
				+ "Well, here's the lowdown:\n" + "u/" + targetUser + " has dropped " + totalComments
				+ " comments, flexing a grand total of " + (totalChars - 1) + " characters.\n"
				+ "Within those, I meticulously counted a whopping `" + totalH + "` *h* or *H* characters!\n\n"
				+ "That brings us to the moment of truth: the legendary H-Score (that's 'h's per every non-'h' character, for the uninitiated) is a solid ***"
				+ hScore + "***, which is a rating of " + rating
				+ "\n\nThis message was brought to you by the H-Counter Bot, report error or issues to the mods or r/hcounterbot";

	}

	/**
	 * Replies to a Reddit comment.
	 *
	 * @param thingId   The ID of the comment to reply to (e.g., "n1lqt5s").
	 * @param replyText The text of the reply.
	 * @return true if the reply was successfully posted, false otherwise.
	 */
	public static boolean replyToComment(String thingId, String replyText) {
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
			}
			return false;
		} catch (Exception e) {
			System.err.println("An error occurred during replyToComment: " + e.getMessage());
			e.printStackTrace();
			return false;
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
	public static boolean markMessagesAsReadBatch(List<String> fullnames) {
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

}
