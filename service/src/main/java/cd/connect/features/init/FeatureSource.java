package cd.connect.features.init;

import cd.connect.features.db.FeatureDb;
import com.bluetrainsoftware.common.config.ConfigKey;
import net.stickycode.stereotype.configured.PostConfigured;
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
  private Map<String, FeatureSourceStatus> features = new HashMap<>();

  @ConfigKey("feature-service.enumClass")
  String enumSource = "";

  @ConfigKey("feature-service.features")
  String inlineSource = "";

  private final FeatureDb featureDatabase;

  public FeatureSource(FeatureDb featureDatabase) {
    this.featureDatabase = featureDatabase;
  }

  @PostConfigured
  public void init() {
    if (enumSource.length() > 0) {
      loadEnumSource();
    } else if ( inlineSource.length() > 0) {
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

      for(Object enumObject: c.getEnumConstants()) {
        Enum enumInstance = Enum.class.cast(enumObject);

        features.put(enumInstance.name(), FeatureSourceStatus.toStatus(enumInstance.toString()));
      }
    } catch (ClassNotFoundException e) {
      log.error("Cannot find enum representing features", e);
      throw new RuntimeException(e);
    }
  }
}
