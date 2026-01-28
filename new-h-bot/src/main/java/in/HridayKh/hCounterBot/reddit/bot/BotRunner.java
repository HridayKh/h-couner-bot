package in.HridayKh.hCounterBot.reddit.bot;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
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
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class BotRunner {
	private static final Logger LOG = Logger.getLogger(BotRunner.class);

	private volatile boolean running = true;

	@Inject
	RedditService redditService;

	@Inject
	Tracer tracer;

	@Blocking
	void onStart(@Observes StartupEvent ev) {
		try {
			// Delay before starting so logs appear correctly when restarting in dev mode
			Thread.sleep(Duration.ofSeconds(5));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			running = false;
		}

		Thread.ofVirtual().name("h-counter-bot-main-loop").start(() -> {
			while (running) {
				try {
					System.out.println("\n\n\n\n\nReddit Bot iteration started.");
					executeIteration();

					// Temporary: Stop after one iteration for testing
					running = false;

					System.out.println("Reddit Bot iteration completed.\n\n\n\n\n");
				} catch (Exception e) {
					LOG.error("Iteration failed", e);
					tracer.spanBuilder("Failed Iteration").startSpan().recordException(e)
							.setStatus(StatusCode.ERROR,
									"Iteration failed due to exception")
							.end();
					try {
						Thread.sleep(Duration.ofSeconds(3));
					} catch (InterruptedException ignored) {
					}
				}

			}
		});
	}

	@WithSpan("redditBot.executeIteration")
	protected void executeIteration() {
		RedditMention[] unreadMentions = redditService.getUnreadComments("username_mention");

		List<String> mentionsToMarkRead = new ArrayList<>();

		for (RedditMention mention : unreadMentions) {
			// sometimes comment replies used to be fetched as mentions in old verisons
			// verify for sanity even though not needed
			if (!mention.getBody().toLowerCase().contains("u/h-counter-bot"))
				continue;

			// remove the mention part from body
			String body = mention.getBody().toLowerCase().replace("u/h-counter-bot", "").trim();

			String targetUser = null;

			if (body.contains("[self]") || body.contains("\\[self\\]"))
				targetUser = mention.getAuthor();
			else if (body.contains("op"))
				targetUser = redditService.getPostOp(mention.getSubreddit(), mention.getPostId());
			else if (body.contains("u/"))
				targetUser = body.split("u/")[1].split(" ")[0];
			else
				targetUser = redditService.getParentRedditor(mention.getParentId());

			if (targetUser == null || targetUser.isBlank() || targetUser.equalsIgnoreCase("[deleted]")) {
				LOG.warn("Could not determine target user for mention: " + mention);
				continue;
			}
			// TODO get last fetched comment and previous score from cache/db
			// TODO assume user is using bot for the first time for now

			String[] bodies = redditService.getUserCommentsBodies(targetUser, "");
			if (bodies == null || bodies.length == 0) {
				LOG.warn("No comments found for user: " + targetUser);
				continue;
			}
			String newNewestScannedCommentId = bodies[0]; // TODO store it

			long totalChars = 0;
			long totalH = 0;
			for (int i = 1; i < bodies.length; i++) {
				String comment = bodies[i];
				totalChars += comment.length();
				totalH += comment.length() - comment.toLowerCase().replace("h", "").length();
			}

			long nonH = totalChars - totalH;
			double hScore = nonH == 0 ? Double.MAX_VALUE : (double) totalH / nonH;
			int totalComments = bodies.length - 1;

			String replyMessage = String.format(
					"Hi u/%s! Here are the h-counter stats for u/%s:\n\n" +
							"- Total Comments Analyzed: %d\n" +
							"- Total 'h' Characters: %d\n" +
							"- Total Other Characters: %d\n" +
							"- H-Score: %.4f\n\n" +
							"^This is a beta version of the h-counter-bot v2. " +
							"See https://www.reddit.com/r/TheLetterH/comments/1qp6i2j" +
							"\n^(I am a bot, this action was performed automatically. " +
							"Contact the mods of r/hcounterbot for any issues.)",
					mention.getAuthor(), targetUser, totalComments, totalH, nonH, hScore);

			// TODO update cache/db with last fetched comment id and new stats

			String mentionNameId = mention.getNameId();
			mentionNameId = mentionNameId.startsWith("t1_") ? mentionNameId : "t1_" + mentionNameId;

			redditService.replyToComment(mentionNameId, replyMessage);
			mentionsToMarkRead.add(mentionNameId);
		}

	}

	@WithSpan("redditBot.WaitForAndUpdateRateLimit")
	public void WaitForAndUpdateRateLimit(Response response) {
		String remainingStr = response.getHeaderString("x-ratelimit-remaining");
		String resetStr = response.getHeaderString("x-ratelimit-reset");
		long remainingRateLimitCallsRemaining = 1L;
		long remainingRateLimitResetTime = 1L;
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

		long remainingTimePerCallSeconds = remainingRateLimitResetTime / remainingRateLimitCallsRemaining;
		long extraBufferMillis = 50L;
		long waitTimeMillis = (remainingTimePerCallSeconds * 1000L) + extraBufferMillis;
		try {
			Thread.sleep(waitTimeMillis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	void onStop(@Observes ShutdownEvent ev) {
		running = false;
	}

}