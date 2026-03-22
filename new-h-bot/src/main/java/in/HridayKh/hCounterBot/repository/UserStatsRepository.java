package in.HridayKh.hCounterBot.repository;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

import in.HridayKh.hCounterBot.model.UserAndStats;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.sortedset.ScoreRange;
import io.quarkus.redis.datasource.sortedset.SortedSetCommands;
import io.quarkus.redis.datasource.sortedset.ZRangeArgs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserStatsRepository {

	private static final int CURRENT_VERSION = 1;
	private static final String LEADERBOARD_KEY = "hBot:leaderboard";

	@Inject
	RedisDataSource redis;

	private SortedSetCommands<@NonNull String, @NonNull String> sortedSet() {
		return redis.sortedSet(String.class, String.class);
	}

	// ── Serialization ─────────────────────────────────────────────────────────

	private String serialize(UserAndStats u) {
		String ranges = u.scannedComments == null || u.scannedComments.length == 0
				? ""
				: String.join(",", u.scannedComments);
		return String.join(";",
				String.valueOf(CURRENT_VERSION),
				u.user(),
				String.valueOf(u.totalComments()),
				String.valueOf(u.totalH()),
				String.valueOf(u.totalNonH()),
				String.valueOf(u.hScore()),
				ranges);
	}

	private UserAndStats deserialize(String raw) {
		String[] parts = raw.split(";", -1);
		int version = Integer.parseInt(parts[0]);
		if (version != CURRENT_VERSION)
			throw new IllegalStateException("Unsupported UserAndStats version: " + version);
		String[] scanned = parts[6].isBlank() ? new String[0] : parts[6].split(",");
		return new UserAndStats(parts[1], scanned,
				Long.parseLong(parts[2]),
				Long.parseLong(parts[3]),
				Long.parseLong(parts[4]),
				Double.parseDouble(parts[5]));
	}

	// ── Write ─────────────────────────────────────────────────────────────────

	public void save(UserAndStats u) {
		// Remove the old entry for this user before reinserting with updated data,
		// since the member string changes when stats change
		deleteByUsername(u.user());
		String serialized = serialize(u);
		if (serialized == null || serialized.isBlank())
			throw new IllegalStateException("Serialized UserAndStats is null or blank");
		sortedSet().zadd(LEADERBOARD_KEY, u.hScore(), serialized);
	}

	private void deleteByUsername(String username) {
		// Find the existing member string for this user and remove it
		// zrangebyscore with a username prefix scan isn't possible, so we track
		// by fetching the current entry via zscore lookup on the raw member —
		// instead, we do a targeted zrange scan for this user's current entry
		List<@NonNull String> all = sortedSet().zrangebyscore(LEADERBOARD_KEY,
				new ScoreRange<>(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
		all.stream()
				.filter(m -> m.split(";", 3)[1].equals(username))
				.findFirst()
				.ifPresent(m -> sortedSet().zrem(LEADERBOARD_KEY, m));
	}

	// ── Read ──────────────────────────────────────────────────────────────────

	public List<UserAndStats> getLeaderboardSlice(int from, int to) {
		return sortedSet().zrange(LEADERBOARD_KEY, from, to, new ZRangeArgs().rev())
				.stream()
				.map(this::deserialize)
				.toList();
	}

	public Optional<UserAndStats> load(String username) {
		List<@NonNull String> all = sortedSet().zrangebyscore(LEADERBOARD_KEY,
				new ScoreRange<>(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
		return all.stream()
				.filter(m -> m.split(";", 3)[1].equals(username))
				.findFirst()
				.map(this::deserialize);
	}

	public List<UserAndStats> search(String q, int offset, int limit) {
		String lowerQ = q.toLowerCase();
		List<@NonNull String> all = sortedSet().zrange(LEADERBOARD_KEY, 0, -1, new ZRangeArgs().rev());
		return all.stream()
				.filter(m -> m.split(";", 3)[1].toLowerCase().contains(lowerQ))
				.skip(offset)
				.limit(limit)
				.map(this::deserialize)
				.toList();
	}

	public long totalCount() {
		return sortedSet().zcard(LEADERBOARD_KEY);
	}

	public long searchCount(String q) {
		String lowerQ = q.toLowerCase();
		List<@NonNull String> all = sortedSet().zrange(LEADERBOARD_KEY, 0, -1, new ZRangeArgs().rev());
		return all.stream()
				.filter(m -> m.split(";", 3)[1].toLowerCase().contains(lowerQ))
				.count();
	}
}