package cd.connect.features.client;

import java.util.EnumSet;
import java.util.List;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public interface FeatureRepository {
  FeatureState getFeatureState(String name);
  void ensureFeaturesExist(Class<? extends Enum> features);
}
