package in.HridayKh.hCounterBot.reddit.model.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TypeT1 {
	public String id;
	public String name;
	public String author;
	public String author_fullname;
	public String subreddit;
	public String subreddit_id;
	public String subreddit_name_prefixed;
	public int score;
	public int ups;
	public int downs;
	public Boolean likes;
	public Double created;
	public Double created_utc;
	public String permalink;
	public Object distinguished;
	public String body;
	public String body_html;
	public String parent_id;
	public String replies;
	public Boolean was_comment;
}
