package in.HridayKh;

import in.HridayKh.hCounterBot.model.RedditComment;
import in.HridayKh.hCounterBot.service.RedditService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

	@Inject
	RedditService rs;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Blocking
	@WithSpan("GET /hello")
	public RedditComment[] hello() {
		RedditComment[] result = rs.getUnreadComments("username_mention");
		return result;
	}
}
