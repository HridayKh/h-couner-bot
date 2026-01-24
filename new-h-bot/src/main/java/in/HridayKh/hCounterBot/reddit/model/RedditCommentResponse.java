package in.HridayKh.hCounterBot.reddit.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RedditCommentResponse {
    @JsonProperty("json")
    private JsonData json;

    pub*-lic JsonData getJson() {
        return json;
    }

    public void setJson(JsonData json) {
        this.json = json;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonIgnoreProperties(ignoreUnknown = true)
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
