package in.HridayKh.hCounterBot.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import in.HridayKh.hCounterBot.model.RedditComment;
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
	public RedditComment[] getUnreadComments(String filterCommentType) {
		Span.current().setAttribute("reddit.filter_type", filterCommentType);
		Log.infof("Fetching unread messages from Reddit (filter: %s)", filterCommentType);

		try {
			String token = handleToken();
			if (token == null)
				throw new RuntimeException("Reddit Bearer Token is Null!");

			Response unreadMessagesResponse = redditClient.getUnreadMessages(token, userAgent);

			RedditListing<TypeT1> listing = unreadMessagesResponse
					.readEntity(new GenericType<RedditListing<TypeT1>>() {
					});

			RedditThing<TypeT1>[] children = listing.data.children;
			Span.current().setAttribute("reddit.items_received", children.length);
			Log.info("processing_unread_comments");

			List<RedditComment> comments = new ArrayList<>();
			for (RedditThing<TypeT1> child : children) {
				TypeT1 comment = child.data;

				if (comment.type == null || !comment.was_comment
						|| !comment.type.equals(filterCommentType)) {
					continue;
				}

				String[] contextParts = comment.context.split("/");
				String postId = contextParts.length > 4 ? contextParts[4] : "unknown";

				comments.add(new RedditComment(
						postId, comment.parent_id, comment.name, comment.author,
						comment.body, comment.type));
			}

			Span.current().setAttribute("reddit.items_matched", comments.size());
			Log.infof("Processed %d unread comments", comments.size());
			Log.info("processing_unread_comments_completed");

			return comments.toArray(new RedditComment[0]);

		} catch (Exception e) {
			Log.errorf(e, "Error fetching unread comments for type: %s", filterCommentType);
			Span.current().recordException(e);
			Span.current().setStatus(StatusCode.ERROR, e.getMessage());
			throw e;
		}
	}
}