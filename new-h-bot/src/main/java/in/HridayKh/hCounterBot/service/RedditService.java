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
import in.HridayKh.hCounterBot.reddit.model.types.TypeT1;
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

	private String handleToken() {
		if (bearerToken != null && !bearerToken.isBlank()
				&& tokenExpiry > (System.currentTimeMillis() + Duration.ofMinutes(5).toMillis()))
			return bearerToken;

		String botIdSecret = botId + ":" + botSecret;
		String botIdSecretBase64 = Base64.getEncoder().encodeToString(botIdSecret.getBytes());
		String basicAuth = "Basic " + botIdSecretBase64;

		String grantType = "password";

		TokenResponse tr = redditClient.getAccessToken(basicAuth, userAgent, grantType, botUser, botPass);

		this.bearerToken = "Bearer " + tr.getAccessToken();
		this.tokenExpiry = (System.currentTimeMillis() + (Duration.ofSeconds(tr.getExpiresIn()).toMillis()));

		return bearerToken;
	}

	public RedditComment[] getUnreadComments(String commentType) {

		RedditListing<TypeT1> listing = redditClient.getUnreadMessages(bearerToken, userAgent);

		RedditListingData<TypeT1> data = listing.data;

		RedditListing<TypeT1>[] children = data.children;

		RedditComment[] comments = new RedditComment[children.length];

		for (TypeT1 ti : children) {
			RedditComment rc = new RedditComment();
			
		}

	}

}
