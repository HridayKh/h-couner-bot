package in.HridayKh;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import in.HridayKh.testApi.PostRestClient;
import in.HridayKh.testApi.RestResource;

@Path("/hello")
public class GreetingResource {

	@Inject
	@RestClient
	PostRestClient postRestClient;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Blocking
	public RestResource hello() throws Exception {
		return postRestClient.getData();
	}
}
