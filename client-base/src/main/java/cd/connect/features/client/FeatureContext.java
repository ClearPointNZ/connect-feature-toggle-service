package cd.connect.features.client;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureContext {
  public static FeatureRepository repository;

  public static boolean isActive(Feature feature) {
    if (repository == null) {
      throw new RuntimeException("You must configure your feature repository before using it.");
    }
    FeatureState fs = repository.getFeatureState(feature.name());
    return fs != null && (fs.whenEnabled != null);
  }
}
