package in.HridayKh.testApi;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/posts")
@RegisterRestClient(configKey = "post-api")
public interface PostRestClient {
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	RestResource getData();
}