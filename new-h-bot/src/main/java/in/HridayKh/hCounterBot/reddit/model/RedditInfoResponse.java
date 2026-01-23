package in.HridayKh.hCounterBot.reddit.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RedditInfoResponse {
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

    public static class ListingData {
        @JsonProperty("children")
        private InfoThing[] children;

        @JsonProperty("after")
        private String after;

        @JsonProperty("before")
        private String before;

        @JsonProperty("modhash")
        private String modhash;

        @JsonProperty("dist")
        private Integer dist;

        public InfoThing[] getChildren() {
            return children;
        }

        public void setChildren(InfoThing[] children) {
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

    public static class InfoThing {
        @JsonProperty("kind")
        private String kind;

        @JsonProperty("data")
        private ThingData data;

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public ThingData getData() {
            return data;
        }

        public void setData(ThingData data) {
            this.data = data;
        }
    }

    public static class ThingData {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("author")
        private String author;

        @JsonProperty("body")
        private String body;

        @JsonProperty("created_utc")
        private Double createdUtc;

        @JsonProperty("subreddit")
        private String subreddit;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public Double getCreatedUtc() {
            return createdUtc;
        }

        public void setCreatedUtc(Double createdUtc) {
            this.createdUtc = createdUtc;
        }

        public String getSubreddit() {
            return subreddit;
        }

        public void setSubreddit(String subreddit) {
            this.subreddit = subreddit;
        }
    }
}
