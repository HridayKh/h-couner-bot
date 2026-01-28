package in.HridayKh.hCounterBot.model;

public class RedditMention {
	private String postId;
	private String subreddit;
	private String parentId;
	private String nameId;
	private String author;
	private String body;

	public RedditMention(String postId, String postSubreddit, String parentId, String nameId, String author,
			String body) {
		this.postId = postId;
		this.subreddit = postSubreddit;
		this.parentId = parentId;
		this.nameId = nameId;
		this.author = author;
		this.body = body;
	}

	public String getPostId() {
		return postId;
	}

	public String getSubreddit() {
		return subreddit;
	}

	public String getParentId() {
		return parentId;
	}

	public String getNameId() {
		return nameId;
	}

	public String getAuthor() {
		return author;
	}

	public String getBody() {
		return body;
	}

	public String toString() {
		return "RedditMention{postId=`" + postId + "`, subreddit=`" + subreddit +
				"`, parentId=`" + parentId + "`, nameId=`" + nameId +
				"`, author=`" + author + "`, body=`" + body + "`}";
	}
}