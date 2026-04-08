package in.HridayKh.hCounterBot.reddit.bot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import in.HridayKh.hCounterBot.model.RedditMention;
import in.HridayKh.hCounterBot.model.UserAndStats;
import in.HridayKh.hCounterBot.reddit.model.ProcessedMentionsReturn;
import in.HridayKh.hCounterBot.repository.UserStatsRepository;
import in.HridayKh.hCounterBot.service.RedditService;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BotLoop {

	@Inject
	RedditService redditService;

	@Inject
	Tracer tracer;

	@Inject
	UserStatsRepository userStatsRepository;

	@WithSpan("redditBot.executeIteration")
	protected void executeIteration() {
		Span iterSpan = Span.current();

		ProcessedMentionsReturn processedMentionsReturn = redditService.getUnreadComments();

		RedditMention[] unreadMentions = processedMentionsReturn.comments();
		RedditMention[] not_mentions_to_mark_read = processedMentionsReturn.comments_not_mentions();

		iterSpan.setAttribute("reddit.unread_mentions_count", unreadMentions.length);

		List<String> mentionsToMarkRead = new ArrayList<>();

		for (RedditMention mention : unreadMentions) {
			String mentionId = processMention(mention);
			if (mentionId == null)
				throw new IllegalStateException("processMention returned null for mention: " + mention);
			mentionsToMarkRead.add(mentionId);
		}

		Log.infof("Marking mentions as read");
		if (!mentionsToMarkRead.isEmpty()) {
			redditService.markMentionsAsRead(mentionsToMarkRead.toArray(new String[0]));
			iterSpan.setAttribute("reddit.mentions_marked_read", mentionsToMarkRead.size());
		}
		Log.info("Marked mentions as read successfully.");

		Log.infof("Marking non mentions as read");
		if (not_mentions_to_mark_read.length > 0)
			redditService.markMentionsAsRead(Arrays.stream(not_mentions_to_mark_read)
					.map((a) -> {
						String b = a.getNameId();
						return b.startsWith("t1_") ? b : "t1_" + b;
					}).toArray(String[]::new));
		Log.info("Marked non mentions as read successfully.");
	}

	@WithSpan("redditBot.processMention")
	String processMention(RedditMention mention) {

		String mentionNameId = mention.getNameId();
		mentionNameId = mentionNameId.startsWith("t1_") ? mentionNameId : "t1_" + mentionNameId;

		if (!mention.getBody().toLowerCase().contains("u/h-counter-bot"))
			return mentionNameId;

		Span mentionSpan = tracer.spanBuilder("redditBot.processMention")
				.setParent(Context.current())
				.startSpan();

		try (Scope scope = mentionSpan.makeCurrent()) {
			mentionSpan.setAttribute("reddit.mention_id", mention.getNameId());
			mentionSpan.setAttribute("reddit.mention_author", mention.getAuthor());
			mentionSpan.setAttribute("reddit.mention_subreddit", mention.getSubreddit());

			String targetUserName = processTargetUserName(mention);
			if (targetUserName == null) {
				Log.warn("Could not determine target user for mention: " + mention);
				mentionSpan.addEvent("mention.skipped",
						Attributes.of(AttributeKey.stringKey("reason"),
								"no_target_user"));
				return mentionNameId;
			}

			mentionSpan.setAttribute("reddit.target_username", targetUserName);

			UserAndStats u = userStatsRepository.load(targetUserName)
					.orElse(new UserAndStats(targetUserName, new String[0], 0, 0, 0, 0));

			String[] bodies = redditService.getUserCommentsBodies(u);
			if (bodies == null || bodies.length == 0) {
				Log.warn("No comments found for user: " + targetUserName);
				mentionSpan.addEvent("mention.skipped",
						Attributes.of(AttributeKey.stringKey("reason"), "no_comments"));
				return mentionNameId;
			}

			long totalChars = u.totalH() + u.totalNonH();
			long totalH = u.totalH();
			for (int i = 1; i < bodies.length; i++) {
				String comment = bodies[i];
				totalChars += comment.length();
				totalH += comment.length() - comment.toLowerCase().replace("h", "").length();
			}

			long totalNonH = totalChars - totalH;
			double hScore = totalNonH == 0 ? Double.MAX_VALUE : (double) totalH / totalNonH;
			long totalComments = u.totalComments() + bodies.length - 1;

			mentionSpan.setAttribute("reddit.new_comments_analyzed", bodies.length - 1);
			mentionSpan.setAttribute("reddit.total_comments", totalComments);
			mentionSpan.setAttribute("reddit.total_h", totalH);
			mentionSpan.setAttribute("reddit.total_non_h", totalNonH);
			mentionSpan.setAttribute("reddit.h_score", hScore);

			String replyMessage = String.format(
					"Hi u/%s! Here are the h-counter stats for u/%s:\n\n" +
							"- Total Comments Analyzed: %d\n" +
							"- Total Characters: %d\n" +
							"- Total 'h' Characters: %d\n" +
							"- Total non-'h' Characters: %d\n" +
							"- H-Score: %.4f\n\n" +
							"^(This is a beta version of the h-counter-bot v2. " +
							"See https://www.reddit.com/r/TheLetterH/comments/1qp6i2j)"
							+
							"\n^(I am a bot, this action was performed automatically. "
							+
							"Contact the mods of r/hcounterbot for any issues.)",
					mention.getAuthor(), targetUserName, totalComments, totalChars, totalH,
					totalNonH, hScore);

			u.updateStats(totalComments, totalH, totalNonH, hScore);
			userStatsRepository.save(u);

			redditService.replyToComment(mentionNameId, replyMessage);
			mentionSpan.addEvent("mention.replied");

			return mentionNameId;

		} catch (Exception e) {
			mentionSpan.recordException(e, Attributes.of(
					AttributeKey.stringKey("reddit.mention_id"), mention.getNameId(),
					AttributeKey.stringKey("reddit.target_author"), mention.getAuthor()));
			mentionSpan.setStatus(StatusCode.ERROR, e.getMessage());
			Log.errorf(e, "Failed to process mention: %s", mention.getNameId());
			return null;
		} finally {
			mentionSpan.end();
		}
	}

	@WithSpan("redditBot.processTargetUserName")
	String processTargetUserName(RedditMention mention) {
		Span span = Span.current();
		span.setAttribute("reddit.mention_id", mention.getNameId());

		String body = mention.getBody().toLowerCase().replace("u/h-counter-bot", "").trim();
		String targetUser = null;
		String resolvedVia;

		if (body.contains("[self]") || body.contains("\\[self\\]") || body.contains("\\\\[self\\\\]")) {
			targetUser = mention.getAuthor();
			resolvedVia = "self";
		} else if (body.contains("[op]") || body.contains("\\[op\\]") || body.contains("\\\\[op\\\\]")) {
			targetUser = redditService.getPostOp(mention.getSubreddit(), mention.getPostId());
			resolvedVia = "op";
		} else if (body.contains("u/")) {
			targetUser = body.split("u/")[1].split(" ")[0];
			resolvedVia = "explicit_mention";
		} else {
			targetUser = redditService.getParentRedditor(mention.getParentId());
			resolvedVia = "parent_comment";
		}

		span.setAttribute("reddit.resolved_via", resolvedVia);

		if (targetUser == null || targetUser.isBlank() || targetUser.equalsIgnoreCase("[deleted]")) {
			span.addEvent("target_user.unresolvable",
					Attributes.of(AttributeKey.stringKey("reddit.resolved_via"), resolvedVia));
			return null;
		}

		span.setAttribute("reddit.target_username", targetUser);
		return targetUser;
	}
}
