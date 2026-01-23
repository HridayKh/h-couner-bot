package in.HridayKh.hCounterBot.reddit.model;

import java.util.List;

public class RedditCommentResponse {
    public RedditCommentJson json;

    public static class RedditCommentJson {
        public List<String> errors;
        public RedditListingResponse.RedditListingData data;
    }
}
