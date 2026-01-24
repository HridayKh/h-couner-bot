package in.HridayKh.hCounterBot.reddit.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
{
    "access_token": "ACCESS_TOKEN_HERE",
    "token_type": "bearer",
    "expires_in": 86400,
    "scope": "*"
}
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenResponse {
	@JsonProperty("access_token")
	private String accessToken;

	@JsonProperty("token_type")
	private String tokenType;

	@JsonProperty("expires_in")
	private long expiresIn;

	@JsonProperty("scope")
	private String scope;

	public String getAccessToken() {
		return accessToken;
	}

	public long getExpiresIn() {
		return expiresIn;
	}
}
