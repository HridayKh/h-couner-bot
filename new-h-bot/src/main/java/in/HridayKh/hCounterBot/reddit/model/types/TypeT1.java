package in.HridayKh.hCounterBot.reddit.model.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TypeT1 {
	public Object first_message;
	public Object first_message_name;
	public String subreddit;
	public Object likes;
	public Object replies;
	public String author_fullname;
	public String id;
	public String subject;
	public Object associated_awarding_id;
	public long score;
	public String author;
	public long num_comments;
	public String parent_id;
	public String subreddit_name_prefixed;
	@JsonProperty("new")
	public boolean new_;
	public String type;
	public String body;
	public String link_title;
	public Object dest;
	public boolean was_comment;
	public String body_html;
	public String name;
	public long created;
	public long created_utc;
	public String context;
	public Object distinguished;

}
