package in.HridayKh.hCounterBot.reddit.model;

import in.HridayKh.hCounterBot.model.RedditMention;

public record ProcessedMentionsReturn(RedditMention[] comments,
		RedditMention[] comments_not_mentions) {
}
