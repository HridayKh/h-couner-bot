package in.HridayKh.hCounterBot.service;

import java.time.Duration;
import java.util.Base64;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import in.HridayKh.hCounterBot.model.RedditComment;
import in.HridayKh.hCounterBot.reddit.RedditClient;
import in.HridayKh.hCounterBot.reddit.model.TokenResponse;
import in.HridayKh.hCounterBot.reddit.model.types.RedditListing;
import in.HridayKh.hCounterBot.reddit.model.types.RedditListingData;
import in.HridayKh.hCounterBot.reddit.model.types.RedditThing;
import in.HridayKh.hCounterBot.reddit.model.types.TypeT1;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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

	@WithSpan("handleToken")
	private String handleToken() {
		Span s = Span.current();
		s.addEvent("Checking token validity");

		if (bearerToken != null && !bearerToken.isBlank()
				&& tokenExpiry > (System.currentTimeMillis() + Duration.ofMinutes(5).toMillis())) {
			s.addEvent("Using cached token");
			return bearerToken;
		}

		s.addEvent("Fetching new access token");
		String botIdSecret = botId + ":" + botSecret;
		String botIdSecretBase64 = Base64.getEncoder().encodeToString(botIdSecret.getBytes());
		String basicAuth = "Basic " + botIdSecretBase64;

		String grantType = "password";

		TokenResponse tr = redditClient.getAccessToken(basicAuth, userAgent, grantType, botUser, botPass);

		this.bearerToken = "Bearer " + tr.getAccessToken();
		this.tokenExpiry = (System.currentTimeMillis() + (Duration.ofSeconds(tr.getExpiresIn()).toMillis()));

		s.addEvent("Token refreshed successfully");
		return bearerToken;
	}

	@WithSpan("getUnreadComments")
	public RedditComment[] getUnreadComments(String commentType) {
		Span s = Span.current();
		s.addEvent("Fetching unread messages");

		RedditListing<TypeT1> listing = redditClient.getUnreadMessages(handleToken(), userAgent);
		RedditListingData<TypeT1> data = listing.data;
		RedditThing<TypeT1>[] children = data.children;
		s.addEvent("Retrieved " + children.length + " unread messages");

		RedditComment[] comments = new RedditComment[children.length];
		int processedCount = 0;
		for (int i = 0; i < children.length; i++) {
			TypeT1 comment = children[i].data;
			if (comment.type == null || !comment.type.equals(commentType))
				continue;
			RedditComment rc = new RedditComment();
			// context = /r/{Subredddit}}/comments/(post_id)/{post_title_slug)/{comment_id}/
			rc.postId = comment.context.split("/")[4];
			rc.parentId = comment.parent_id;
			rc.nameId = comment.name;
			rc.author = comment.author;
			rc.body = comment.body;
			rc.type = comment.type;
			comments[i] = rc;
			processedCount++;
		}
		s.addEvent("Processed " + processedCount + " comments of type: " + commentType);
		return comments;
	}

}
