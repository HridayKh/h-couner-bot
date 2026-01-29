package in.HridayKh.hCounterBot.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import in.HridayKh.hCounterBot.model.RedditMention;
import in.HridayKh.hCounterBot.reddit.RedditClient;
import in.HridayKh.hCounterBot.reddit.model.TokenResponse;
import in.HridayKh.hCounterBot.reddit.model.types.RedditListing;
import in.HridayKh.hCounterBot.reddit.model.types.RedditThing;
import in.HridayKh.hCounterBot.reddit.model.types.TypeT1;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class RedditService {

	@Inject
	@RestClient
	RedditClient redditClient;

	@ConfigProperty(name = "reddit.user-agent")
	String userAgent;

	@ConfigProperty(name = "reddit.bot-username")
	String botUser;

	@ConfigProperty(name = "reddit.bot-password")
	String botPass;

	@ConfigProperty(name = "reddit.bot-id")
	String botId;

	@ConfigProperty(name = "reddit.bot-secret")
	String botSecret;

	private String bearerToken;
	private long tokenExpiry;

	@WithSpan("redditService.auth.handleToken")
	String handleToken() {
		long fiveMinutesFromNow = System.currentTimeMillis() + Duration.ofMinutes(5).toMillis();

		if (bearerToken != null && !bearerToken.isBlank() && tokenExpiry > fiveMinutesFromNow) {
			Log.info("token_valid");
			return bearerToken;
		}

		try {
			Log.info("Reddit access token expired or missing; refreshing...");
			Log.info("token_refresh_started");

			String botIdSecret = botId + ":" + botSecret;
			String botIdSecretBase64 = Base64.getEncoder().encodeToString(botIdSecret.getBytes());
			String basicAuth = "Basic " + botIdSecretBase64;

			// does not count towards reddit rate limit
			Response trResponse = redditClient.getAccessToken(basicAuth, userAgent, "password", botUser,
					botPass);

			TokenResponse tr = trResponse.readEntity(new GenericType<TokenResponse>() {
			});

			this.bearerToken = "Bearer " + tr.getAccessToken();
			this.tokenExpiry = System.currentTimeMillis()
					+ Duration.ofSeconds(tr.getExpiresIn()).toMillis();

			Log.info("Reddit token refreshed successfully");
			Log.info("token_refresh_completed");
			return bearerToken;

		} catch (Exception e) {
			Log.error("Failed to refresh Reddit token", e);
			Log.error("token_refresh_failed");
			Span.current().recordException(e);
			Span.current().setStatus(StatusCode.ERROR, "Failed to refresh token");
			throw new RuntimeException("Authentication failed", e);
		}
	}

	@WithSpan("redditService.rateLimit.waitAndUpdate")
	void WaitForAndUpdateRateLimit(Response response) {
		String remainingStr = response.getHeaderString("x-ratelimit-remaining");
		String resetStr = response.getHeaderString("x-ratelimit-reset");
		float remainingRateLimitCallsRemaining = 1.0f;
		float remainingRateLimitResetTime = 1.0f;
		if (remainingStr != null) {
			try {
				remainingRateLimitCallsRemaining = Float.parseFloat(remainingStr);
			} catch (NumberFormatException e) {
				Log.warn("Failed to parse x-ratelimit-remaining: " + remainingStr, e);
				Span.current().setStatus(StatusCode.ERROR, "Failed to parse x-ratelimit-remaining");
				Span.current().recordException(e);
			}
		}
		if (resetStr != null) {
			try {
				remainingRateLimitResetTime = Float.parseFloat(resetStr);
			} catch (NumberFormatException e) {
				Log.warn("Failed to parse x-ratelimit-reset: " + resetStr, e);
				Span.current().setStatus(StatusCode.ERROR, "Failed to parse x-ratelimit-remaining");
				Span.current().recordException(e);
			}
		}

		float remainingTimePerCallSeconds = (remainingRateLimitResetTime / remainingRateLimitCallsRemaining) * 1000.0f;
		long extraBufferMillis = 10L;
		long waitTimeMillis = ((long) remainingTimePerCallSeconds) + extraBufferMillis;
		Log.info(remainingTimePerCallSeconds);
		Log.infof("Remaining: %.2f, Reset: %.2f s, Waiting time: %d ms",
				remainingRateLimitCallsRemaining, remainingRateLimitResetTime, waitTimeMillis);

		try {
			Thread.sleep(waitTimeMillis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@WithSpan("redditService.comments.getUnread")
	public RedditMention[] getUnreadComments() {
		Log.infof("Fetching unread messages from Reddit");

		try {
			String token = handleToken();
			if (token == null)
				throw new RuntimeException("Reddit Bearer Token is Null!");

			Response unreadMessagesResponse = redditClient.getUnreadMessages(token, userAgent);
			WaitForAndUpdateRateLimit(unreadMessagesResponse);

			RedditListing<TypeT1> listing = unreadMessagesResponse
					.readEntity(new GenericType<RedditListing<TypeT1>>() {
					});

			RedditThing<TypeT1>[] children = listing.data.children;
			Span.current().setAttribute("reddit.items_received", children.length);
			Log.infof("processing %d unread_comments", children.length);

			List<RedditMention> comments = new ArrayList<>();
			for (RedditThing<TypeT1> child : children) {
				TypeT1 comment = child.data;

				if (!comment.was_comment || !comment.body.toLowerCase().contains("u/h-counter-bot"))
					continue;

				String[] contextParts = comment.context.split("/");
				String postId = contextParts.length > 4 ? contextParts[4] : "unknown";

				String author = comment.author;
				author = author.startsWith("u/") ? author.substring(2) : author;

				comments.add(new RedditMention(
						postId, comment.subreddit, comment.parent_id, comment.name,
						author, comment.body));
			}

			Span.current().setAttribute("reddit.items_matched", comments.size());
			Log.infof("Processed %d unread comments", comments.size());
			Log.info("processing_unread_comments_completed");

			return comments.toArray(new RedditMention[0]);

		} catch (Exception e) {
			Log.errorf(e, "Error fetching unread comments");
			Span.current().recordException(e);
			Span.current().setStatus(StatusCode.ERROR, e.getMessage());
			throw e;
		}
	}

	@WithSpan("redditService.targetUser.identifier.getPostOp")
	public String getPostOp(String subreddit, String postId) {
		Span.current().setAttribute("reddit.post_id", postId);
		Log.infof("Fetching OP username for post: r/%s/%s", subreddit, postId);
		try {
			String token = handleToken();
			if (token == null)
				throw new RuntimeException("Reddit Bearer Token is Null!");
			Response postResponse = redditClient.getPost(token, userAgent, subreddit, postId);
			WaitForAndUpdateRateLimit(postResponse);

			RedditListing<TypeT1>[] postListing = postResponse
					.readEntity(new GenericType<RedditListing<TypeT1>[]>() {
					});

			String author = postListing[0].data.children[0].data.author.toLowerCase();
			Log.infof("Fetched OP username for post %s: %s", postId, author);

			return author.startsWith("u/") ? author.substring(2) : author;

		} catch (Exception e) {
			Log.errorf(e, "Error fetching OP username for post: %s", postId);
			Span.current().recordException(e);
			Span.current().setStatus(StatusCode.ERROR, e.getMessage());
			throw e;
		}
	}

	@WithSpan("redditService.targetUser.identifier.getParentRedditor")
	public String getParentRedditor(String parentId) {
		Span.current().setAttribute("reddit.parent_id", parentId);
		Log.infof("Fetching username for parent: %s", parentId);
		try {
			String token = handleToken();
			if (token == null)
				throw new RuntimeException("Reddit Bearer Token is Null!");
			Response postResponse = redditClient.getInfo(token, userAgent, parentId);
			WaitForAndUpdateRateLimit(postResponse);

			RedditListing<TypeT1> infoListing = postResponse
					.readEntity(new GenericType<RedditListing<TypeT1>>() {
					});

			String author = infoListing.data.children[0].data.author.toLowerCase();

			Log.infof("Fetched username for parent %s: %s", parentId, author);
			return author.startsWith("u/") ? author.substring(2) : author;

		} catch (Exception e) {
			Log.errorf(e, "Error fetching username for parent: %s", parentId);
			Span.current().recordException(e);
			Span.current().setStatus(StatusCode.ERROR, e.getMessage());
			throw e;
		}
	}

	@WithSpan("redditService.targetUser.getUserCommentsBodies")
	public String[] getUserCommentsBodies(String username, String newestScannedCommentId) {
		Span.current().setAttribute("reddit.target_username", username);
		Log.infof("Fetching comments for user: %s", username);
		try {
			String token = handleToken();
			if (token == null)
				throw new RuntimeException("Reddit Bearer Token is Null!");

			List<String> commentsBodies = new ArrayList<>();
			String before = newestScannedCommentId == null ? "" : newestScannedCommentId;
			before = before.toLowerCase().startsWith("t1_") || before.isBlank() ? before : "t1_" + before;

			int redditMaxLimit = 100;
			int maxPagesPerUser = 10;

			int pages = 0;
			String currentAfter = "";
			boolean foundScannedComment = false;
			while (!foundScannedComment) {
				// only apply the comment limit if we are noo scanning after a scanned comment
				if (before.isBlank() && pages >= maxPagesPerUser)
					break;

				Response response = redditClient.getUserComments(token, userAgent, username,
						redditMaxLimit, "", currentAfter);
				WaitForAndUpdateRateLimit(response);

				RedditListing<TypeT1> listing = response
						.readEntity(new GenericType<RedditListing<TypeT1>>() {
						});

				if (listing.data.children == null || listing.data.children.length == 0)
					break;

				for (RedditThing<TypeT1> child : listing.data.children) {
					if (commentsBodies.size() < 1)
						commentsBodies.add(child.data.name);

					if (child.data.name.equals(newestScannedCommentId)) {
						foundScannedComment = true;
						break;
					}

					commentsBodies.add(child.data.body);
				}
				currentAfter = listing.data.after;
				pages++;

				if (currentAfter == null || currentAfter.isBlank())
					break;
			}

			Log.infof("Fetched %d comments for user: %s", commentsBodies.size() - 1, username);
			return commentsBodies.toArray(new String[0]);

		} catch (Exception e) {
			Log.errorf(e, "Error fetching comments for user: %s", username);
			Span.current().recordException(e);
			Span.current().setStatus(StatusCode.ERROR, e.getMessage());
			throw e;
		}
	}

	@WithSpan("redditService.comments.replyToComment")
	public void replyToComment(String parentCommentId, String replyText) {
		try {
			String token = handleToken();
			if (token == null)
				throw new RuntimeException("Reddit Bearer Token is Null!");

			Response replyResponse = redditClient.replyToComment(token, userAgent, parentCommentId,
					replyText, "json");
			WaitForAndUpdateRateLimit(replyResponse);

			Log.infof("Replied to comment %s successfully.", parentCommentId);
		} catch (Exception e) {
			Log.errorf(e, "Error replying to comment: %s", parentCommentId);
			Span.current().recordException(e);
			Span.current().setStatus(StatusCode.ERROR, e.getMessage());
			throw new RuntimeException("Failed to reply to comment: " + parentCommentId, e);
		}
	}

	@WithSpan("redditService.comments.markMentionsAsRead")
	public void markMentionsAsRead(String[] mentionsToBeMarkedRead) {
		StringBuilder sb = new StringBuilder();
		for (String mentionId : mentionsToBeMarkedRead) {
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(mentionId);
		}
		Log.infof("Marking mentions as read: %s", sb.toString());

		String token = handleToken();
		if (token == null)
			throw new RuntimeException("Reddit Bearer Token is Null!");

		Response response = redditClient.markMessagesAsRead(token, userAgent, sb.toString());
		WaitForAndUpdateRateLimit(response);
		Log.info("Marked mentions as read successfully.");
	}
}
