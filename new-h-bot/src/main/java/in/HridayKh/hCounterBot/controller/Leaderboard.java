package in.HridayKh.hCounterBot.controller;

import java.util.List;
import java.util.stream.IntStream;

import org.jspecify.annotations.NonNull;

import in.HridayKh.hCounterBot.model.LeaderboardRow;
import in.HridayKh.hCounterBot.model.UserAndStats;
import in.HridayKh.hCounterBot.repository.UserStatsRepository;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class Leaderboard {

	private static final int PAGE_SIZE = 25;

	@Inject
	UserStatsRepository repo;

	@Inject
	Template index;

	@Inject
	Template board;

	@GET
	@Produces(MediaType.TEXT_HTML)
	public TemplateInstance index() {
		return index.instance();
	}

	@GET
	@Path("/board")
	@Produces(MediaType.TEXT_HTML)
	public TemplateInstance board(
			@QueryParam("q") @DefaultValue("") String q,
			@QueryParam("page") @DefaultValue("1") int page) {

		int offset = (page - 1) * PAGE_SIZE;

		List<UserAndStats> users = q.isBlank()
				? repo.getLeaderboardSlice(offset, offset + PAGE_SIZE - 1)
				: repo.search(q, offset, PAGE_SIZE);

		long total = q.isBlank() ? repo.totalCount() : repo.searchCount(q);
		int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);

		List<@NonNull LeaderboardRow> entries = IntStream.range(0, users.size())
				.mapToObj(i -> new in.HridayKh.hCounterBot.model.LeaderboardRow(offset + i + 1,
						users.get(i)))
				.toList();

		return board.data("entries", entries)
				.data("page", page)
				.data("totalPages", totalPages)
				.data("total", total)
				.data("q", q)
				.data("prevPage", page - 1)
				.data("nextPage", page + 1)
				.data("hasPrev", page > 1)
				.data("hasNext", page < totalPages)
				.data("showFirstEllipsis", page > 2)
				.data("showLastEllipsis", page < totalPages - 1);
	}

}