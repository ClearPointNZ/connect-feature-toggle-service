package cd.connect.features.inmemory;

import cd.connect.features.api.FeatureDb;
import cd.connect.features.api.FeatureSourceStatus;
import cd.connect.features.api.FeatureState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class InMemoryDb implements FeatureDb {
  private static final Logger log = LoggerFactory.getLogger(InMemoryDb.class);
  Map<String, FeatureState> features = new ConcurrentHashMap<>();
  List<Consumer<WatchSignal>> watchers = new ArrayList<>();

  @Override
  public void ensureExists(Map<String, FeatureSourceStatus> map) {
    map.forEach((k, v) -> {
      if (features.get(k) == null) {
        FeatureState value = new FeatureState(k, v == FeatureSourceStatus.ENABLED ? LocalDateTime.now() : null,
          v == FeatureSourceStatus.LOCKED);
        log.info("registering feature: {}", value);
        features.put(k, value);
      }
    });
  }

  @Override
  public void ensureExists(List<String> features) {
    features.forEach(f -> this.features.putIfAbsent(f, new FeatureState(f, null, true)));
  }

  @Override
  public void watch(Consumer<WatchSignal> consumer) {
    watchers.add(consumer);
  }

  @Override
  public Map<String, FeatureState> getFeatures() {
    HashMap<String, FeatureState> f = new HashMap<>();
    f.putAll(features);
    return f;
  }

  @Override
  public FeatureState getFeature(String featureName) {
    return features.get(featureName);
  }

  @Override
  public void refresh() {
  }

  @Override
  public void apply(FeatureState featureState) {
    features.put(featureState.getName(), featureState);
    WatchSignal ws = new WatchSignal(featureState.getName(), featureState, false);
    watchers.forEach(c -> c.accept(ws));
  }

  @Override
  public void init() {
  }
}
