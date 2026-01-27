package in.HridayKh.hCounterBot.model;

public class RedditComment {
	private String postId;
	private String parentId;
	private String nameId;
	private String author;
	private String body;
	private String type;

	public RedditComment(String postId, String parentId, String nameId, String author, String body, String type) {
		this.postId = postId;
		this.parentId = parentId;
		this.nameId = nameId;
		this.author = author;
		this.body = body;
		this.type = type;
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

	public String getType() {
		return type;
	}

	public String toString() {
		return "RedditComment{" +
				"postId='" + postId + '\'' +
				", parentId='" + parentId + '\'' +
				", nameId='" + nameId + '\'' +
				", author='" + author + '\'' +
				", body='" + body + '\'' +
				", type='" + type + '\'' +
				'}';
	}
}