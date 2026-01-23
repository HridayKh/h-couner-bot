package in.HridayKh.hCounterBot.reddit.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RedditCommentResponse {
    @JsonProperty("json")
    private JsonData json;

    public JsonData getJson() {
        return json;
    }

    public void setJson(JsonData json) {
        this.json = json;
    }

    public static class JsonData {
        @JsonProperty("errors")
        private Object[] errors;

        @JsonProperty("data")
        private DataDetails data;

        public Object[] getErrors() {
            return errors;
        }

        public void setErrors(Object[] errors) {
            this.errors = errors;
        }

        public DataDetails getData() {
            return data;
        }

        public void setData(DataDetails data) {
            this.data = data;
        }
    }

    public static class DataDetails {
        @JsonProperty("things")
        private Object[] things;

        public Object[] getThings() {
            return things;
        }

        public void setThings(Object[] things) {
            this.things = things;
        }
    }
}
