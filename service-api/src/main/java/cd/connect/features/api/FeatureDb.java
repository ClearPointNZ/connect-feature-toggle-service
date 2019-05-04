package cd.connect.features.api;

import cd.connect.features.api.FeatureSourceStatus;
import cd.connect.features.api.FeatureState;

import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public interface FeatureDb {
  void ensureExists(Map<String, FeatureSourceStatus> features);

  void watch(Consumer<WatchSignal> changed);

  /**
   * Get an immutable map of the current features
   *
   * @return
   */
  Map<String, FeatureState> getFeatures();

  /**
   * Return a copy of the feature as it currently stands.
   *
   * @param name - feature to find
   * @return - feature copy or null.
   */
  FeatureState getFeature(String name);

  /**
   * request from the client for us to refresh from the database. This may be relevant if we are using a
   * non-watched database.
   */
  void refresh();

  void apply(FeatureState featureState);


  // this is here because stickycode does not necessarily initialize in the dependency order
  void init();

  class WatchSignal {
    String name;
    FeatureState state;
    boolean deleted; // otherwise changed/added, look in state for state

    public WatchSignal(String name, FeatureState state, boolean deleted) {
      this(name, deleted);

      this.state = state;
    }

    public WatchSignal(String name, boolean deleted) {
      this.name = name;
      this.deleted = deleted;
    }

    public String getName() {
      return name;
    }

    public FeatureState getState() {
      return state;
    }

    public boolean isDeleted() {
      return deleted;
    }

    @Override
    public String toString() {
      return "WatchSignal{" +
        "name='" + name + '\'' +
        ", state=" + state +
        ", deleted=" + deleted +
        '}';
    }
  }
}
