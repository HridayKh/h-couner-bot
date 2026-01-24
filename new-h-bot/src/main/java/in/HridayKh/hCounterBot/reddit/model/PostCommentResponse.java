package in.HridayKh.hCounterBot.reddit.model;

import in.HridayKh.hCounterBot.reddit.model.types.RedditListing;
import in.HridayKh.hCounterBot.reddit.model.types.TypeT1;

public class PostCommentResponse {
	public Json json;

	public class Json {
		public Object errors;
		public Data data;
	}

	public class Data {
		public RedditListing<TypeT1>[] things;
	}
}
