package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {

	public static final String USER_AGENT = "script_in_java:h-counter-bot:v1.1 (by u/PROMAN8625)";
	public static final String CLIENT_ID = System.getenv("h_bot_id");
	public static final String CLIENT_SECRET = System.getenv("h_bot_secret");
	public static final String REDDIT_USERNAME = System.getenv("h_bot_username");
	public static final String REDDIT_PASSWORD = System.getenv("h_bot_pass");

	public static String TOKEN = "";
	public static Set<String> processedMessageFullnames = new HashSet<>();

	private static final String PROCESSED_IDS_FILE = System.getenv("h_bot_file");
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
					Thread.sleep(sleepDuration);
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
			String[] newTokenArr = TokenAuth.getAccessToken();
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

		List<String[]> unreadMentions = MentionsManager.getUnreadMessagesAndFilterMentions(fullnamesToMarkAsRead);

		for (String[] mention : unreadMentions) {
			if (mention.length < 4) {
				System.err.println("Skipping malformed mention array: " + String.join(",", mention));
				continue;
			}

			String id = mention[0]; // The base ID (e.g., "n1lqt5s")
			String author = mention[1]; // The author of the mention
			String targetUser = mention[2]; // The user being queried (e.g., "PROMAN8625")
			String fullname = mention[3]; // The full ID (e.g., "t1_n1lqt5s")

			System.out.println("Processing Mention ID: " + id + ", Author: " + author + ", Target User: " + targetUser);

			try {
				// Fetch comments for the target user
				String[] comments = GetRedditorComments.getComments(targetUser);
				long[] inf = GetRedditorComments.parseCommentH(comments);
				String result = CommentReply.determineResult(inf, comments.length, author, targetUser);
				boolean success = CommentReply.replyToComment(id, result);
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
			boolean batchMarked = CommentReply.markMessagesAsReadBatch(fullnamesToMarkAsRead);
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
