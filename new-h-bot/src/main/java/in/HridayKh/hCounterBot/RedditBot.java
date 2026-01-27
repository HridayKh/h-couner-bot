package in.HridayKh.hCounterBot;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;

import in.HridayKh.hCounterBot.model.RedditMention;
import in.HridayKh.hCounterBot.service.RedditService;

import java.time.Duration;

@ApplicationScoped
public class RedditBot {
	private static final Logger LOG = Logger.getLogger(RedditBot.class);

	private volatile boolean running = true;

	private int remainingRateLimitCallsRemaining = 1;
	private long remainingRateLimitResetTime = Duration.ofSeconds(10).toMillis();

	@Inject
	RedditService redditService;

	@Blocking
	void onStart(@Observes StartupEvent ev) {
		try {
			// Delay before starting so logs appear correctly when restarting in dev mode
			Thread.sleep(Duration.ofSeconds(5));

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			running = false;
		}
		Thread.ofVirtual().name("h-counter-bot-main-loop").start(this::runLoop);
	}

	void runLoop() {
		while (running) {
			try {
				System.out.println("\n\n\n\n\nReddit Bot iteration started.");
				executeIteration();
				running = false; // Temporary: Stop after one iteration for testing
				System.out.println("Reddit Bot iteration completed.\n\n\n\n\n");
			} catch (Exception e) {
				LOG.error("Iteration failed", e);
				try {
					Thread.sleep(Duration.ofSeconds(3));
				} catch (InterruptedException ignored) {
				}
			}

		}
	}

	@WithSpan("redditBot.updateRateLimitInfo")
	public void updateRateLimitInfo(Response response) {
		String remainingStr = response.getHeaderString("x-ratelimit-remaining");
		String resetStr = response.getHeaderString("x-ratelimit-reset");
		if (remainingStr != null) {
			try {
				remainingRateLimitCallsRemaining = (int) Float.parseFloat(remainingStr);
			} catch (NumberFormatException e) {
				LOG.warn("Failed to parse x-ratelimit-remaining: " + remainingStr, e);
				Span.current().setStatus(StatusCode.ERROR, "Failed to parse x-ratelimit-remaining");
				Span.current().recordException(e);
			}
		}
		if (resetStr != null) {
			try {
				remainingRateLimitResetTime = (int) Float.parseFloat(resetStr);
			} catch (NumberFormatException e) {
				LOG.warn("Failed to parse x-ratelimit-reset: " + resetStr, e);
				Span.current().setStatus(StatusCode.ERROR, "Failed to parse x-ratelimit-remaining");
				Span.current().recordException(e);
			}
		}
	}

	@WithSpan("redditBot.waitForRateLimit")
	public void waitForRateLimit() {
		long remainingTimePerCallSeconds = remainingRateLimitResetTime / remainingRateLimitCallsRemaining;
		long extraBufferMillis = 500L;
		long waitTimeMillis = (remainingTimePerCallSeconds * 1000L) + extraBufferMillis;
		try {
			Thread.sleep(waitTimeMillis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@WithSpan("redditBot.executeIteration")
	protected void executeIteration() {
		RedditMention[] unreadMentions = redditService.getUnreadComments("username_mention");

		for (RedditMention mention : unreadMentions) {
			// sometimes comment replies used to be fetched as mentions in old verisons
			// verify for sanity even though not needed
			if (!mention.getBody().toLowerCase().contains("u/h-counter-bot")) 
				continue;
			
			// remove the mention part from body
			String body = mention.getBody().toLowerCase().replace("u/h-counter-bot", "").trim();

			String targetUser = null;

			if (body.contains("[self]") || body.contains("\\[self\\]")) {
				String author = mention.getAuthor();
				targetUser = author.startsWith("u/") ? author.substring(2) : author;
			} else if (body.contains("op")) {
				String postId = mention.getPostId();
				// TODO redditService.getPostOP returns username without u/ prefix
				// getPostOP uses redditClient.getPost and extracts the op from it
			} else if (body.contains("u/")) {
				targetUser = body.split("u/")[1].split(" ")[0];
			} else {
				String parentId = mention.getParentId();
				// TODO redditService.getParentRedditor returns username without u/ prefix
				// getParentRedditor uses redditClient.getInfo to get the author of the parent
			}
			if (targetUser == null || targetUser.isBlank()) {
				LOG.warn("Could not determine target user for mention: " + mention);
				continue;
			}
			// TODO get last fetched comment and previous score from cache/db
			// assume user is using bot for the first time for now

			// TODO redditService.getRedditorComments(afterCommentId, targetUser) to get comments
			// getRedditorComments uses redditClient.getUserComments to fetch comments using pagination

			// TODO this.calculateHScore(String[] comments) to calculate h-score from comments and other stats

			// update cache/db with last fetched comment id and new stats

			// reply to the mention
		}

	}

	void onStop(@Observes ShutdownEvent ev) {
		running = false;
	}

}