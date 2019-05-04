package cd.connect.features.resource.jersey;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@Path("/_status/healthz")
public class HealthResource {
	@GET
	public String healthz() {
		return "ok";
	}
}
