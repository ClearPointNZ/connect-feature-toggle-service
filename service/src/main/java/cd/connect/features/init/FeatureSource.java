package cd.connect.features.init;

import cd.connect.features.api.FeatureDb;
import cd.connect.features.api.FeatureSourceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureSource {
	private static final Logger log = LoggerFactory.getLogger(FeatureSource.class);
	private final FeatureDb featureDatabase;
	private Map<String, FeatureSourceStatus> features = new HashMap<>();
	private final String inlineSource;

	public FeatureSource(FeatureDb featureDatabase, String inlineSource) {
		this.featureDatabase = featureDatabase;
		this.inlineSource = inlineSource;
		
    if (inlineSource.length() > 0) {
			loadInlineSource();
		}

		if (featureDatabase != null) {
			featureDatabase.init(); // ensure it is initialized
      if (features.size() > 0) {
        featureDatabase.ensureExists(features);
      }
		}
	}

	private void loadInlineSource() {
		String source = inlineSource.trim();

		Arrays.stream(source.split(",")).map(String::trim).filter(s -> s.length() > 0).forEach(s -> {
				if (s.contains(":")) {
					List<String> parts = Arrays.stream(s.split(":")).map(String::trim).filter(p -> p.length() > 0).collect(Collectors.toList());

					if (parts.size() == 2) {
						features.put(parts.get(0), FeatureSourceStatus.toStatus(parts.get(1)));
					} else {
						log.warn("ignoring feature {} as it is not in  the format feature:label", s);
					}
				} else {
					features.put(s, FeatureSourceStatus.toStatus(s));
				}
			}
		);
	}

	public Map<String, FeatureSourceStatus> getFeatures() {
		return features;
	}
}
