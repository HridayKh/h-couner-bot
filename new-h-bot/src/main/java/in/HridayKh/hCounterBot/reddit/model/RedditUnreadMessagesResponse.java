package in.HridayKh.hCounterBot.reddit.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RedditUnreadMessagesResponse {
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
        private MessageThing[] children;

        @JsonProperty("after")
        private String after;

        @JsonProperty("before")
        private String before;

        @JsonProperty("modhash")
        private String modhash;

        @JsonProperty("dist")
        private Integer dist;

        public MessageThing[] getChildren() {
            return children;
        }

        public void setChildren(MessageThing[] children) {
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

    public static class MessageThing {
        @JsonProperty("kind")
        private String kind;

        @JsonProperty("data")
        private MessageData data;

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public MessageData getData() {
            return data;
        }

        public void setData(MessageData data) {
            this.data = data;
        }
    }

    public static class MessageData {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("author")
        private String author;

        @JsonProperty("body")
        private String body;

        @JsonProperty("was_comment")
        private Boolean wasComment;

        @JsonProperty("type")
        private String type;

        @JsonProperty("context")
        private String context;

        @JsonProperty("parent_id")
        private String parentId;

        @JsonProperty("created_utc")
        private Double createdUtc;

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

        public Boolean getWasComment() {
            return wasComment;
        }

        public void setWasComment(Boolean wasComment) {
            this.wasComment = wasComment;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        public Double getCreatedUtc() {
            return createdUtc;
        }

        public void setCreatedUtc(Double createdUtc) {
            this.createdUtc = createdUtc;
        }
    }
}
