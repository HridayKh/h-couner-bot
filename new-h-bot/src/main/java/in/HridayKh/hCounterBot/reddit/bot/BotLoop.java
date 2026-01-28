package in.HridayKh.hCounterBot.reddit.bot;

import java.util.ArrayList;
import java.util.List;

import in.HridayKh.hCounterBot.model.RedditMention;
import in.HridayKh.hCounterBot.service.RedditService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BotLoop {

	@Inject
	RedditService redditService;

	@WithSpan("redditBot.executeIteration")
	protected void executeIteration() {
		RedditMention[] unreadMentions = redditService.getUnreadComments();

		List<String> mentionsToMarkRead = new ArrayList<>();

		for (RedditMention mention : unreadMentions) {
			// sometimes comment replies used to be fetched as mentions in old verisons
			// verify for sanity even though not needed
			if (!mention.getBody().toLowerCase().contains("u/h-counter-bot"))
				continue;

			String targetUser = processTargetUser(mention);
			if (targetUser == null) {
				Log.warn("Could not determine target user for mention: " + mention);
				continue;
			}

			// TODO get last fetched comment and previous score from cache/db
			// TODO assume user is using bot for the first time for now
			String prevNewestScannedCommentId = "";
			long prevTotalComments = 0;
			long prevTotalH = 0;
			long prevTotalChars = 0;

			String[] bodies = redditService.getUserCommentsBodies(targetUser, prevNewestScannedCommentId);
			if (bodies == null || bodies.length == 0) {
				Log.warn("No comments found for user: " + targetUser);
				continue;
			}
			// String newNewestScannedCommentId = bodies[0]; // TODO store it

			long totalChars = prevTotalChars;
			long totalH = prevTotalH;
			for (int i = 1; i < bodies.length; i++) {
				String comment = bodies[i];
				totalChars += comment.length();
				totalH += comment.length() - comment.toLowerCase().replace("h", "").length();
			}

			long nonH = totalChars - totalH;
			double hScore = nonH == 0 ? Double.MAX_VALUE : (double) totalH / nonH;
			long totalComments = prevTotalComments + bodies.length - 1;

			String replyMessage = String.format(
					"Hi u/%s! Here are the h-counter stats for u/%s:\n\n" +
							"- Total Comments Analyzed: %d\n" +
							"- Total Characters: %d\n" +
							"- Total 'h' Characters: %d\n" +
							"- Total non-'h' Characters: %d\n" +
							"- H-Score: %.4f\n\n" +
							"^(This is a beta version of the h-counter-bot v2. " +
							"See https://www.reddit.com/r/TheLetterH/comments/1qp6i2j)" +
							"\n^(I am a bot, this action was performed automatically. " +
							"Contact the mods of r/hcounterbot for any issues.)",
					mention.getAuthor(), targetUser, totalComments, totalChars, totalH, nonH, hScore);

			// TODO update cache/db with last fetched comment id and new stats

			String mentionNameId = mention.getNameId();
			mentionNameId = mentionNameId.startsWith("t1_") ? mentionNameId : "t1_" + mentionNameId;

			redditService.replyToComment(mentionNameId, replyMessage);
			mentionsToMarkRead.add(mentionNameId);
		}
		if (!mentionsToMarkRead.isEmpty())
			redditService.markMentionsAsRead(mentionsToMarkRead.toArray(new String[0]));
	}

	String processTargetUser(RedditMention mention) {
		// remove the mention part from body
		String body = mention.getBody().toLowerCase().replace("u/h-counter-bot", "").trim();

		String targetUser = null;

		if (body.contains("[self]") || body.contains("\\[self\\]")|| body.contains("\\\\[self\\\\]")) {
			targetUser = mention.getAuthor();
		} else if (body.contains("[op]") || body.contains("\\[op\\]")|| body.contains("\\\\[op\\\\]")) {
			targetUser = redditService.getPostOp(mention.getSubreddit(), mention.getPostId());
		} else if (body.contains("u/")) {
			targetUser = body.split("u/")[1].split(" ")[0];
		} else {
			targetUser = redditService.getParentRedditor(mention.getParentId());
		}

		if (targetUser.isBlank() || targetUser.equalsIgnoreCase("[deleted]"))
			return null;

		return targetUser;
	}

}
