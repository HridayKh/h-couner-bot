package in.HridayKh.hCounterBot.reddit.model;

import java.util.List;

public class RedditListingResponse {
    public String kind;
    public RedditListingData data;

    public static class RedditListingData {
        public String after;
        public String before;
        public String modhash;
        public List<RedditThing> children;
    }
}
