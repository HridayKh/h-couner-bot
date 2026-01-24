package in.HridayKh.hCounterBot.reddit;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TokenHandler {
	private String accessToken;
	private long expiryTimeMillis;

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public long getExpiryTimeMillis() {
		return expiryTimeMillis;
	}

	public void setExpiryTimeMillis(long expiryTimeMillis) {
		this.expiryTimeMillis = expiryTimeMillis;
	}
}
