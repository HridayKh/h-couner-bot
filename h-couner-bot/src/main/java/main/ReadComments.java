package main;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ReadComments {

	/**
	 * Fetches unread messages from Reddit and filters them to return only username
	 * mentions. It also adds messages (mentions or non-mentions) to a list to be
	 * marked as read in a batch.
	 *
	 * @param fullnamesToMarkAsRead A list to which fullnames of processed messages
	 *                              will be added.
	 * @return A List of String arrays, where each array contains [id, author,
	 *         targetUser, fullname].
	 */
	public static List<String[]> getUnreadMessagesAndFilterMentions(List<String> fullnamesToMarkAsRead) {
		List<String[]> filteredMentions = new ArrayList<>();
		try {
			URL url = new URI("https://oauth.reddit.com/message/unread").toURL();
			String responseBody = HttpUtil.performHttpRequest("GET", url, null, true, false);

			if (responseBody != null) {
				JSONObject jsonResponse = new JSONObject(responseBody);

				if (jsonResponse.has("data")) {
					JSONObject data = jsonResponse.getJSONObject("data");
					if (data.has("children")) {
						JSONArray children = data.getJSONArray("children");
						filteredMentions = parseComments(children, fullnamesToMarkAsRead);
					}
				}
			}
		} catch (IOException | URISyntaxException | InterruptedException e) {
			System.err.println("An unexpected error occurred in getUnreadMessagesAndFilterMentions: " + e.getMessage());
			e.printStackTrace();
		}
		return filteredMentions;

	}

	private static List<String[]> parseComments(JSONArray children, List<String> fullnamesToMarkAsRead) {
		List<String[]> filteredMentions = new ArrayList<>();
		for (int i = 0; i < children.length(); i++) {

			JSONObject message = children.getJSONObject(i).getJSONObject("data");
			String messageFullname = message.getString("name");
			String messageId = message.getString("id");

			if (Main.processedMessageFullnames.contains(messageFullname)) {
				System.out.println("Skipping already processed message: " + messageFullname);
				fullnamesToMarkAsRead.add(messageFullname);
				continue;
			}

			boolean isComment = message.has("was_comment") && message.getBoolean("was_comment");
			boolean isMention = message.getString("body").contains("u/h-counter-bot");

			if (!isComment && !isMention) {
				System.out.println("Skipping non-mention message: " + messageFullname + " (Type: "
						+ (message.has("type") ? message.getString("type") : "N/A") + ", WasComment: "
						+ (message.has("was_comment") ? message.getBoolean("was_comment") : "N/A") + ", Body: "
						+ message.getString("body") + ")");
				fullnamesToMarkAsRead.add(messageFullname);
				Main.processedMessageFullnames.add(messageFullname);
			}

			String author = message.getString("author");
			String body = message.getString("body").toLowerCase().replace("u/h-counter-bot", "").trim();
			String targetUser;

			if (body.contains("[self]")) {
				targetUser = author;
			} else if (body.contains("u/")) {
				int start = body.indexOf("u/") + 2;
				int end = body.indexOf(" ", start);
				if (end == -1) {
					end = body.length();
				}
				targetUser = body.substring(start, end);
				targetUser = targetUser.replaceAll("[^a-zA-Z0-9_-]", "");
			} else if (body.contains("op")) {
				targetUser = getOP(message.getString("context"));

			} else {
				targetUser = getParentRedditor(message.getString("parent_id"));
			}
			if (targetUser == null) {
				System.err.println("Could not determine target user for mention ID: " + messageId + ". Skipping.");
				fullnamesToMarkAsRead.add(messageFullname);
				Main.processedMessageFullnames.add(messageFullname);
				continue;
			}
			filteredMentions.add(new String[] { messageId, author, targetUser, messageFullname });
		}

		return filteredMentions;
	}

	/**
	 * Retrieves the person who was asked info or, that may be OP or a commentor of
	 * a submission given a parentId.
	 *
	 * @param parentId The context URL of a comment (e.g.,
	 *                 "/r/subreddit/comments/post_id/comment_id/?context=3").
	 * @return The username of the original poster, or null if not found or an error
	 *         occurs.
	 */
	private static String getParentRedditor(String parentId) {
		try {
			URL url = new URI("https://oauth.reddit.com/api/info.json?id=" + parentId).toURL();
			String responseBody = HttpUtil.performHttpRequest("GET", url, null, false, false);

			if (responseBody != null && !responseBody.isEmpty()) {
				JSONObject jsonResponse = new JSONObject(responseBody);

				JSONArray children = jsonResponse.getJSONObject("data").getJSONArray("children");

				if (children.length() > 0) {
					JSONObject itemData = children.getJSONObject(0).getJSONObject("data");
					if (itemData.has("author")) {
						String author = itemData.getString("author");
						if (author.equals("[deleted]")) {
							System.out.println("Author of " + parentId + " is deleted.");
							return null;
						}
						return author;
					} else {
						System.err.println("Author field not found for Reddit ID: " + parentId);
						return null;
					}
				} else {
					System.err.println("No data found for Reddit ID: " + parentId + " in API response.");
					return null;
				}

			} else {
				System.err.println("Empty or null response body for Reddit ID: " + parentId);
				return null;
			}
		} catch (Exception e) {
			System.err.println("An unexpected error occurred while getting author for Reddit ID " + parentId + ": "
					+ e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	private static String getOP(String context) {
		String op = null;
		try {
			// Example context: /r/subreddit/comments/post_id/comment_id/
			String[] parts = context.strip().split("/");
			// Ensure enough parts to extract subreddit and post ID
			if (parts.length < 5) {
				System.err.println("Invalid context for getOP (too short): " + context);
				return null;
			}
			String subreddit = parts[2];
			String postId = parts[4]; // The post_id

			URL url;
			url = new URI("https://www.reddit.com/r/" + subreddit + "/comments/" + postId + "/.json").toURL();

			JSONArray jsonArrayResponse;
			jsonArrayResponse = new JSONArray(HttpUtil.performHttpRequest("GET", url, null, false, false));
			// The first element of the array is the submission itself
			JSONObject submissionData = jsonArrayResponse.getJSONObject(0).getJSONObject("data")
					.getJSONArray("children").getJSONObject(0).getJSONObject("data");
			op = submissionData.getString("author");
		} catch (JSONException | IOException | URISyntaxException | InterruptedException e) {
			e.printStackTrace();
		}

		return op;
	}
}
