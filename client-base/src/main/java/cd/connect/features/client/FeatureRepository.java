package cd.connect.features.client;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public interface FeatureRepository {
  FeatureState getFeatureState(String name);
}
