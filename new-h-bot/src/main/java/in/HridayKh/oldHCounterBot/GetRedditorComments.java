package in.HridayKh.oldHCounterBot;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class GetRedditorComments {
	/**
	 * Calculates total characters and total h characters in an array of comments.
	 *
	 * @param comments An array of comment strings.
	 * @return A long array where index 0 is total characters and index 1 is total h
	 *         characters.
	 */
	public static long[] parseCommentH(String[] comments) {
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
	public static String[] getComments(String author) {
		List<String> allComments = new ArrayList<>();
		String after = null;
		boolean hasMoreComments = true;
		int limit = 100; // This is the requested limit per page
		int pages = 0; // This is the number of pages fetched
		while (hasMoreComments) {
			if (pages >= 10) {
				hasMoreComments = false; // Stop after 10 pages to avoid excessive requests
				break;
			}
			StringBuilder urlBuilder = new StringBuilder("https://oauth.reddit.com/user/").append(author)
					.append("/comments/.json?limit=").append(limit);

			if (after != null) {
				urlBuilder.append("&after=").append(after);
			}

			String url = urlBuilder.toString();

			try {
				URL apiUrl = new URI(url).toURL();
				String responseBody = HttpUtil.performHttpRequest("GET", apiUrl, null, false);
				pages++; // Increment the page count for each request
				if (responseBody != null) {
					JSONObject jsonResponse = new JSONObject(responseBody);
					JSONObject data = jsonResponse.getJSONObject("data");
					JSONArray commentsArray = data.getJSONArray("children");

					// Always add the comments from the current page
					for (int i = 0; i < commentsArray.length(); i++) {
						allComments.add(commentsArray.getJSONObject(i).getJSONObject("data").getString("body"));
					}

					// Get the 'after' value for the next request
					after = data.optString("after", null); // Use optString to handle null gracefully

					// If 'after' is null or empty, it means there are no more pages
					if (after == null || after.isEmpty()) {
						hasMoreComments = false;
					}
					// If commentsArray.length() is 0, it means we got an empty page, so no more
					// comments
					if (commentsArray.length() == 0) {
						hasMoreComments = false;
					}

				} else {
					System.err.println("Empty response body for user " + author);
					hasMoreComments = false; // Stop if response is null
				}
			} catch (URISyntaxException | IOException | InterruptedException e) {
				System.err.println("Error fetching user comments for " + author + ": " + e.getMessage());
				e.printStackTrace();
				hasMoreComments = false; // Stop on error
			}
		}
		return allComments.toArray(new String[0]);
	}

}
