package cd.connect.features;

import cd.connect.features.resource.FeatureResource;
import cd.connect.features.resource.HealthResource;
import io.prometheus.client.exporter.MetricsServlet;

import java.util.stream.Stream;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class JerseyModule extends cd.connect.spring.jersey.JerseyModule {
	@Override
	protected Stream<Class<?>> registerResources() {
		return Stream.of(FeatureResource.class, HealthResource.class);
	}

	@Override
	protected String getUrlServletPrefix() {
		return "/*";
	}

	@Override
	public void register() {
		servlet(MetricsServlet.class, s -> s.name("metrics").priority(10).url("/_status/metrics"));
		register(Stream.of(FeatureResource.class, HealthResource.class));
	}
}
