package cd.connect.features.init;

import cd.connect.features.db.FeatureDb;
import com.bluetrainsoftware.common.config.ConfigKey;
import com.bluetrainsoftware.common.config.PreStart;

import javax.inject.Inject;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureSourceConfigured {
	@ConfigKey("feature-service.enumClass")
	String enumSource = "";

	@ConfigKey("feature-service.features")
	String inlineSource = "";

	private FeatureSource featureSource;
	private final FeatureDb featureDb;

	@Inject
	public FeatureSourceConfigured(FeatureDb featureDb) {
		this.featureDb = featureDb;
	}

	@PreStart
	public void init() {
		featureSource = new FeatureSource(featureDb, enumSource, inlineSource);
	}
}
