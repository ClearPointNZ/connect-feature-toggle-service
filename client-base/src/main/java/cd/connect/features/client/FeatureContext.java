package cd.connect.features.client;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureContext {
	public static final String ACCELERATE_FEATURE_OVERRIDE = "accelerate-feature-override";
	static final String FEATURE_TOGGLES_ALLOW_OVERRIDE = "feature-toggles.allow-override";
	public static final String FEATURE_TOGGLES_PREFIX = "feature-toggles.";
	public static FeatureRepository repository;

  public static boolean isActive(Feature feature) {
    if (System.getProperty(FEATURE_TOGGLES_ALLOW_OVERRIDE) != null ) {
	    String override = Optional.ofNullable(GlobalTracer.get())
		    .map(Tracer::activeSpan)
		    .filter(Objects::nonNull)
		    .map(span -> span.getBaggageItem(ACCELERATE_FEATURE_OVERRIDE)).orElse(null);

	    if (override != null) {
		    if (override.contains(String.format("%s=true", feature.name()))) {
			    return true;
		    }
		    if (override.contains(String.format("%s=false", feature.name()))) {
			    return false;
		    }
	    }
    }

    String devOverride = System.getProperty(FEATURE_TOGGLES_PREFIX + feature.name());
    if (devOverride != null) {
    	return Boolean.parseBoolean(devOverride);
    }

	  if (repository == null) {
		  throw new RuntimeException("You must configure your feature repository before using it.");
	  }
//		  .map(override -> ).orElse(false);

    FeatureState fs = repository.getFeatureState(feature.name());
    return fs != null && (fs.whenEnabled != null);
  }
}
