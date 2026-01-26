package in.HridayKh;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import in.HridayKh.hCounterBot.service.RedditService;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/hello")
public class GreetingResource {

	@Inject
	@RestClient
	RedditService r;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Blocking
	public Response hello() {
		return Response.ok(r.getUnreadComments("username_mention")[0].nameId).build();
	}
}
