package in.HridayKh.hCounterBot.reddit;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import in.HridayKh.hCounterBot.reddit.model.PostCommentResponse;
import in.HridayKh.hCounterBot.reddit.model.TokenResponse;
import in.HridayKh.hCounterBot.reddit.model.types.RedditListing;
import in.HridayKh.hCounterBot.reddit.model.types.TypeT1;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/")
@RegisterRestClient(configKey = "reddit-client")
public interface RedditClient {

	@POST
	@Path("/api/v1/access_token")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	TokenResponse getAccessToken(@HeaderParam("Authorization") String basicAuth,
			@HeaderParam("User-Agent") String userAgent,
			@FormParam("grant_type") String grantType,
			@FormParam("username") String username,
			@FormParam("password") String password);

	@POST
	@Path("/api/comment")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	PostCommentResponse replyToComment(@HeaderParam("Authorization") String bearerToken,
			@HeaderParam("User-Agent") String userAgent,
			@FormParam("thing_id") String thingId,
			@FormParam("text") String text,
			@FormParam("api_type") String apiType);

	@POST
	@Path("/api/read_message")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	void markMessagesAsRead(@HeaderParam("Authorization") String bearerToken,
			@HeaderParam("User-Agent") String userAgent,
			@FormParam("id") String ids);

	@GET
	@Path("/user/{author}/comments/.json")
	RedditListing<TypeT1> getUserComments(@HeaderParam("Authorization") String bearerToken,
			@HeaderParam("User-Agent") String userAgent,
			@PathParam("author") String author,
			@QueryParam("limit") int limit,
			@QueryParam("after") String after);

	@GET
	@Path("/message/unread")
	RedditListing<TypeT1> getUnreadMessages(@HeaderParam("Authorization") String bearerToken,@HeaderParam("User-Agent") String userAgent);

	@GET
	@Path("/api/info")
	RedditListing<TypeT1> getInfo(@HeaderParam("Authorization") String bearerToken,
			@HeaderParam("User-Agent") String userAgent,
			@QueryParam("id") String id);

	@GET
	@Path("/r/{subreddit}/comments/{postId}/")
	RedditListing<TypeT1>[] getPostComments(
			@HeaderParam("Authorization") String bearerToken,
			@HeaderParam("User-Agent") String userAgent,
			@PathParam("subreddit") String subreddit,
			@PathParam("postId") String postId);

}
