package cd.connect.features.init;

/**
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public enum FeatureSourceStatus {
  LOCKED, // cannot be enabled unless unlocked
  UNLOCKED, // can be locked or enabled
  ENABLED; // can be disabled (goes back to unlocked)

  static FeatureSourceStatus toStatus(String val) {
    if (val != null) {
      val = val.trim().toUpperCase();
      if (LOCKED.toString().equals(val)) {
        return LOCKED;
      } else if (UNLOCKED.toString().equals(val)) {
        return UNLOCKED;
      } else if (ENABLED.toString().equals(val)) {
        return ENABLED;
      }
    }

    return defaultStatus();
  }

  static FeatureSourceStatus defaultStatus() {
    return LOCKED;
  }
}
