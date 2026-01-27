package in.HridayKh.hCounterBot.model;

public class RedditMention {
	private String postId;
	private String parentId;
	private String nameId;
	private String author;
	private String body;

	public RedditMention(String postId, String parentId, String nameId, String author, String body) {
		this.postId = postId;
		this.parentId = parentId;
		this.nameId = nameId;
		this.author = author;
		this.body = body;
	}

	public String getPostId() {
		return postId;
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
		return "RedditMention{" +
				"postId='" + postId + '\'' +
				", parentId='" + parentId + '\'' +
				", nameId='" + nameId + '\'' +
				", author='" + author + '\'' +
				", body='" + body + '\'' +
				'}';
	}
}