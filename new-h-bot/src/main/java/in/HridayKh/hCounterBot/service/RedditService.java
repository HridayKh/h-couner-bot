package in.HridayKh.hCounterBot.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import in.HridayKh.hCounterBot.model.RedditMention;
import in.HridayKh.hCounterBot.reddit.RedditClient;
import in.HridayKh.hCounterBot.reddit.bot.BotRunner;
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

	@Inject
	BotRunner redditBot;

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

		System.out.println("Token Expiry: " + tokenExpiry + ", Current Time: " + System.currentTimeMillis()
				+ ", Five Minutes From Now: " + fiveMinutesFromNow);

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

	@WithSpan("redditService.comments.getUnread")
	public RedditMention[] getUnreadComments(String filterCommentType) {
		Span.current().setAttribute("reddit.filter_type", filterCommentType);
		Log.infof("Fetching unread messages from Reddit (filter: %s)", filterCommentType);

		try {
			String token = handleToken();
			if (token == null)
				throw new RuntimeException("Reddit Bearer Token is Null!");

			Response unreadMessagesResponse = redditClient.getUnreadMessages(token, userAgent);
			redditBot.WaitForAndUpdateRateLimit(unreadMessagesResponse);

			RedditListing<TypeT1> listing = unreadMessagesResponse
					.readEntity(new GenericType<RedditListing<TypeT1>>() {
					});

			RedditThing<TypeT1>[] children = listing.data.children;
			Span.current().setAttribute("reddit.items_received", children.length);
			Log.info("processing_unread_comments");

			List<RedditMention> comments = new ArrayList<>();
			for (RedditThing<TypeT1> child : children) {
				TypeT1 comment = child.data;

				if (comment.type == null || !comment.was_comment
						|| !comment.type.equals(filterCommentType)) {
					continue;
				}

				String[] contextParts = comment.context.split("/");
				String postId = contextParts.length > 4 ? contextParts[4] : "unknown";

				String author = comment.author;
				author = author.startsWith("u/") ? author.substring(2) : author;

				comments.add(new RedditMention(
						postId, comment.subreddit, comment.parent_id, comment.name,
						author,
						comment.body));
			}

			Span.current().setAttribute("reddit.items_matched", comments.size());
			Log.infof("Processed %d unread comments", comments.size());
			Log.info("processing_unread_comments_completed");

			return comments.toArray(new RedditMention[0]);

		} catch (Exception e) {
			Log.errorf(e, "Error fetching unread comments for type: %s", filterCommentType);
			Span.current().recordException(e);
			Span.current().setStatus(StatusCode.ERROR, e.getMessage());
			throw e;
		}
	}

	@WithSpan("redditService.targetUser.identifier.getPostOp")
	public String getPostOp(String subreddit, String postId) {
		Span.current().setAttribute("reddit.post_id", postId);
		Log.infof("Fetching OP username for post: %s", postId);
		try {
			String token = handleToken();
			if (token == null)
				throw new RuntimeException("Reddit Bearer Token is Null!");
			Response postResponse = redditClient.getPost(token, userAgent, subreddit, postId);
			redditBot.WaitForAndUpdateRateLimit(postResponse);

			// returned is an array with first item listing of post (t3)
			// and second item listing of comments (t1)
			RedditListing<TypeT1> postListing = postResponse
					.readEntity(new GenericType<RedditListing<TypeT1>[]>() {
					})[0];

			String author = postListing.data.children[0].data.author.toLowerCase();
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
		Log.infof("Fetching OP username for parent: %s", parentId);
		try {
			String token = handleToken();
			if (token == null)
				throw new RuntimeException("Reddit Bearer Token is Null!");
			Response postResponse = redditClient.getInfo(token, userAgent, parentId);
			redditBot.WaitForAndUpdateRateLimit(postResponse);

			parentId = parentId.startsWith("t1_") ? parentId : "t1_" + parentId;

			RedditListing<TypeT1> infoListing = postResponse
					.readEntity(new GenericType<RedditListing<TypeT1>>() {
					});

			String author = infoListing.data.children[0].data.author.toLowerCase();

			Log.infof("Fetched OP username for parent %s: %s", parentId, author);
			return author.startsWith("u/") ? author.substring(2) : author;

		} catch (Exception e) {
			Log.errorf(e, "Error fetching OP username for parent: %s", parentId);
			Span.current().recordException(e);
			Span.current().setStatus(StatusCode.ERROR, e.getMessage());
			throw e;
		}
	}

	@WithSpan("redditService.targetUser.getUserCommentsBodies")
	public String[] getUserCommentsBodies(String username, String nesestScannedCommentId) {
		Span.current().setAttribute("reddit.target_username", username);
		Log.infof("Fetching comments for user: %s", username);
		try {
			String token = handleToken();
			if (token == null)
				throw new RuntimeException("Reddit Bearer Token is Null!");

			List<String> commentsBodies = new ArrayList<>();
			String before = nesestScannedCommentId == null ? "" : nesestScannedCommentId;
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
				redditBot.WaitForAndUpdateRateLimit(response);

				RedditListing<TypeT1> listing = response
						.readEntity(new GenericType<RedditListing<TypeT1>>() {
						});

				if (listing.data.children == null || listing.data.children.length == 0)
					break;

				for (RedditThing<TypeT1> child : listing.data.children) {
					if (commentsBodies.size() < 1)
						commentsBodies.add(child.data.name);

					if (child.data.name.equals(nesestScannedCommentId)) {
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
			redditBot.WaitForAndUpdateRateLimit(replyResponse);

			Log.infof("Replied to comment %s successfully.", parentCommentId);
		} catch (Exception e) {
			Log.errorf(e, "Error replying to comment: %s", parentCommentId);
			Span.current().recordException(e);
			Span.current().setStatus(StatusCode.ERROR, e.getMessage());
			throw new RuntimeException("Failed to reply to comment: " + parentCommentId, e);
		}
	}
}