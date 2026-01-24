package in.HridayKh.hCounterBot.reddit.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RedditThing {
    public String kind;
    public RedditThingData data;

    public static class RedditThingData {
        public String id;
        public String name; // fullname
        public String author;
        public String body;
        
        @JsonProperty("parent_id")
        public String parentId;
        
        @JsonProperty("was_comment")
        public boolean wasComment;
        
        public String context;
        
        // Add other fields as needed
        public String type;
    }
}
