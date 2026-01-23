package in.HridayKh.hCounterBot.reddit.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RedditTokenResponse {
    @JsonProperty("access_token")
    public String accessToken;

    @JsonProperty("expires_in")
    public int expiresIn;
}
