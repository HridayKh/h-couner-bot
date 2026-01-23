package in.HridayKh.hCounterBot.reddit.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RedditListingResponse {
    @JsonProperty("kind")
    private String kind;

    @JsonProperty("data")
    private ListingData data;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public ListingData getData() {
        return data;
    }

    public void setData(ListingData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListingData {
        @JsonProperty("children")
        private RedditThing[] children;

        @JsonProperty("after")
        private String after;

        @JsonProperty("before")
        private String before;

        @JsonProperty("modhash")
        private String modhash;

        @JsonProperty("dist")
        private Integer dist;

        public RedditThing[] getChildren() {
            return children;
        }

        public void setChildren(RedditThing[] children) {
            this.children = children;
        }

        public String getAfter() {
            return after;
        }

        public void setAfter(String after) {
            this.after = after;
        }

        public String getBefore() {
            return before;
        }

        public void setBefore(String before) {
            this.before = before;
        }

        public String getModhash() {
            return modhash;
        }

        public void setModhash(String modhash) {
            this.modhash = modhash;
        }

        public Integer getDist() {
            return dist;
        }

        public void setDist(Integer dist) {
            this.dist = dist;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RedditThing {
        @JsonProperty("kind")
        private String kind;

        @JsonProperty("data")
        private Object data;

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }
}
