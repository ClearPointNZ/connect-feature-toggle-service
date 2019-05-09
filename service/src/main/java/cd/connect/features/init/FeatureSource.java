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
	private final String enumSource;
	private final String inlineSource;

	public FeatureSource(FeatureDb featureDatabase, String enumSource, String inlineSource) {
		this.featureDatabase = featureDatabase;
		this.enumSource = enumSource;
		this.inlineSource = inlineSource;
		
		if (enumSource.length() > 0) {
			loadEnumSource();
		} else if (inlineSource.length() > 0) {
			loadInlineSource();
		} else {
			log.error("There are no sources of features.");
			throw new RuntimeException("There are no sources of features.");
		}

		if (featureDatabase != null) {
			featureDatabase.init(); // ensure it is initialized
			featureDatabase.ensureExists(features);
		}
	}

	public void init() {
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

	private void loadEnumSource() {
		try {
			Class c = Class.forName(enumSource);

			for (Object enumObject : c.getEnumConstants()) {
				Enum enumInstance = Enum.class.cast(enumObject);

				features.put(enumInstance.name(), FeatureSourceStatus.toStatus(enumInstance.toString()));
			}
		} catch (ClassNotFoundException e) {
			log.error("Cannot find enum representing features", e);
			throw new RuntimeException(e);
		}
	}
}
