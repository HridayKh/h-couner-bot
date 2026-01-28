package in.HridayKh.hCounterBot.reddit;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
@RegisterRestClient(configKey = "reddit-api")
public interface RedditClient {

	@POST
	@Path("/api/v1/access_token")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	Response getAccessToken(@HeaderParam("Authorization") String basicAuth,
			@HeaderParam("User-Agent") String userAgent,
			@FormParam("grant_type") String grantType,
			@FormParam("username") String username,
			@FormParam("password") String password);

	@GET
	@Path("/message/unread")
	Response getUnreadMessages(@HeaderParam("Authorization") String bearerToken,
			@HeaderParam("User-Agent") String userAgent);

	@GET
	@Path("/r/{subreddit}/comments/{postId}/")
	Response getPost(
			@HeaderParam("Authorization") String bearerToken,
			@HeaderParam("User-Agent") String userAgent,
			@PathParam("subreddit") String subreddit,
			@PathParam("postId") String postId);

	@GET
	@Path("/api/info")
	Response getInfo(@HeaderParam("Authorization") String bearerToken,
			@HeaderParam("User-Agent") String userAgent,
			@QueryParam("id") String id);

	@GET
	@Path("/user/{author}/comments")
	Response getUserComments(@HeaderParam("Authorization") String bearerToken,
			@HeaderParam("User-Agent") String userAgent,
			@PathParam("author") String author,
			@QueryParam("limit") int limit,
			@QueryParam("before") String before,
			@QueryParam("after") String after);

	@POST
	@Path("/api/comment")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	Response replyToComment(@HeaderParam("Authorization") String bearerToken,
			@HeaderParam("User-Agent") String userAgent,
			@FormParam("thing_id") String thingId,
			@FormParam("text") String text,
			@FormParam("api_type") String apiType);

	@POST
	@Path("/api/read_message")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	Response markMessagesAsRead(@HeaderParam("Authorization") String bearerToken,

			@HeaderParam("User-Agent") String userAgent,

			@FormParam("id") String ids);

}
