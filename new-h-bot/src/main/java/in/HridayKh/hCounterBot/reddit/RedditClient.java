package in.HridayKh.hCounterBot.reddit;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import in.HridayKh.hCounterBot.reddit.model.RedditCommentResponse;
import in.HridayKh.hCounterBot.reddit.model.RedditListingResponse;
import in.HridayKh.hCounterBot.reddit.model.RedditTokenResponse;
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
    RedditTokenResponse getAccessToken(@HeaderParam("Authorization") String basicAuth,
            @FormParam("grant_type") String grantType,
            @FormParam("username") String username,
            @FormParam("password") String password);

    @POST
    @Path("/api/comment")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    RedditCommentResponse replyToComment(@HeaderParam("Authorization") String bearerToken,
            @FormParam("thing_id") String thingId,
            @FormParam("text") String text,
            @FormParam("api_type") String apiType);

    @POST
    @Path("/api/read_message")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    void markMessagesAsRead(@HeaderParam("Authorization") String bearerToken,
            @FormParam("id") String ids);

    @GET
    @Path("/user/{author}/comments/.json")
    RedditListingResponse getUserComments(@HeaderParam("Authorization") String bearerToken,
            @PathParam("author") String author,
            @QueryParam("limit") int limit,
            @QueryParam("after") String after);

    @GET
    @Path("/message/unread")
    RedditListingResponse getUnreadMessages(@HeaderParam("Authorization") String bearerToken);

    @GET
    @Path("/api/info.json")
    RedditListingResponse getInfo(@HeaderParam("Authorization") String bearerToken,
            @QueryParam("id") String id);

    @GET
    @Path("/r/{subreddit}/comments/{postId}/.json")
    List<RedditListingResponse> getPostComments(@HeaderParam("Authorization") String bearerToken,
            @PathParam("subreddit") String subreddit,
            @PathParam("postId") String postId);

}
