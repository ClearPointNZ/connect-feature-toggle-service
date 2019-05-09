package cd.connect.features.db;

import cd.connect.features.api.FeatureSourceStatus;
import cd.connect.features.api.FeatureState;
import cd.connect.jackson.JacksonObjectProvider;
import com.bluetrainsoftware.common.config.ConfigKey;
import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Watch;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.exception.ClosedWatcherException;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.kv.PutResponse;
import com.coreos.jetcd.watch.WatchEvent;
import com.coreos.jetcd.watch.WatchResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import net.stickycode.stereotype.configured.PostConfigured;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * This is an etcd version of the backing-store. This is preferred because we can watch it for updates,
 * and it is master-master replicated in the cluster and persistent.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureDbEtcd implements FeatureDb {
  private static final Logger log = LoggerFactory.getLogger(FeatureDbEtcd.class);
  private final static TypeReference<Set<String>> STRING_SET = new TypeReference<Set<String>>() {
  };
  private final ObjectMapper mapper = JacksonObjectProvider.mapper;
  private final Map<String, FeatureState> states = new ConcurrentHashMap<>();
  private final ExecutorService watchPool = Executors.newCachedThreadPool();
  private final Set<Consumer<WatchSignal>> signalListener;
  private final List<Watch.Watcher> inflightWatchers = new ArrayList<>();
  @ConfigKey("feature-service.etcd.url")
  String connectionUrl = "http://localhost:2379";
  @ConfigKey("feature-service.etcd.offset")
  String offset = "/connect-cd/features";
  @ConfigKey("feature-service.etcd.enabled")
  Boolean enabled = Boolean.FALSE;
  private KV kvClient;
  private Client etcdClient;
  private Watch watchClient;

  @Inject
  public FeatureDbEtcd(Set<Consumer<WatchSignal>> signalListener) {
    this.signalListener = signalListener;
  }

  @PostConfigured
  public void init() {
    if (enabled && etcdClient == null) {
      etcdClient = Client.builder().endpoints(connectionUrl).build();
      kvClient = etcdClient.getKVClient();
      watchClient = etcdClient.getWatchClient();

      if (offset.endsWith("/")) {
        offset = offset.substring(0, offset.length() - 1);
      }
    }

    // make sure when we die that we close all of the inflight watchers
    Runtime.getRuntime().addShutdownHook(new Thread(() -> inflightWatchers.forEach(Watch.Watcher::close)));
  }

  public String fnOffset(String name) {
    return offset + "/" + name;
  }

  /**
   * Check to see if the key contains the features we have just been passed, and if so, remove them.
   * <p>
   * If we try and remove a feature that doesn't exist, then we have to re-write the list of features, but
   * we want to do that after we have written the key updates.
   *
   * @param foundFeatures - the whole list of features, found ones removed
   * @param features      - the json array of feature names (not statuses)
   * @return - whether we should update the list of features
   */
  private boolean determineNewFeatures(Map<String, FeatureSourceStatus> foundFeatures, String features) {
    boolean updateFeatureListRequired = false;

    log.info("looking for existing features (some exist)");

    Set<String> featuresNames = featureNamesFromJson(features);
    for (String fn : featuresNames) {
      // this loads the feature name in so that it can be deleted out and notified of deletion later if it changes
      // - this is done in loadFeatures() - first up it issues a delete for all features it doesn't now know about
      states.put(fn, new FeatureState());

      FeatureSourceStatus removeFeature = foundFeatures.remove(fn);
      if (removeFeature == null) {
        log.info("delete required: {}", fn);
        updateFeatureListRequired = true;
      }
    }

    if (foundFeatures.size() > 0) { // new features
      log.info("new features found numbering {}", foundFeatures.size());
      updateFeatureListRequired = true;
    } else {
      log.info("no new features exist, all ready.");
    }

    return updateFeatureListRequired;
  }


  protected CompletableFuture<GetResponse> kvGet(String key) {
    return kvClient.get(new ByteSequence(key));
  }

  protected CompletableFuture<PutResponse> kvPut(String key, String value) {
    return kvClient.put(new ByteSequence(key), new ByteSequence(value));
  }

  @Override
  public void ensureExists(Map<String, FeatureSourceStatus> features) {
    Map<String, FeatureSourceStatus> foundFeatures = new HashMap<>();
    foundFeatures.putAll(features);

    try {
      kvGet(offset).thenApply(resp -> {
        // drop all features that exist.
        boolean updateFeatureListRequired = true; // default that if the key isn't found, we need to re-write

        if (!resp.getKvs().isEmpty()) {
          updateFeatureListRequired = determineNewFeatures(foundFeatures, resp.getKvs().get(0).getValue().toStringUtf8());
        }

        if (updateFeatureListRequired) {
          // these are new features, so we now need to save them
          saveNewFeatures(foundFeatures);

          return rewriteMasterFeatureList(features.keySet());
        }

        return resp;
      }).get();
    } catch (InterruptedException | ExecutionException e) {
      log.error("Failed to ensure features exist", e);
      throw new RuntimeException(e);
    }

    loadFeatures(features.keySet());

    watchForFeatureNameChanges();

    watchForFeatureChanges(features.keySet());
  }

  protected void watchForFeatureNameChanges() {
    watchPool.submit(this::watchFeatureNamesChange);
  }

  protected void watchForFeatureChanges(Set<String> featureNames) {

    // now kill any and all feature watchers
    inflightWatchers.forEach(Watch.Watcher::close);

    // now go and watch all of the features again
    featureNames.forEach(fn -> {
      watchPool.submit(() -> {
        watchFeatureStateChange(fn);
      });
    });
  }

  private Set<String> featureNamesFromJson(String features) {
    try {
      return mapper.readValue(features, STRING_SET);
    } catch (IOException e) {
      log.error("Cannot decode features {}", features, e);
      throw new RuntimeException("Unable to decode features", e);
    }
  }

  private FeatureState featureStateFromJson(String fState) {
    try {
      return mapper.readValue(fState, FeatureState.class);
    } catch (IOException e) {
      log.error("Cannot decode feature state `{}`", fState, e);
      throw new RuntimeException("Unable to decode features", e);
    }
  }

  private String featureStateToJson(FeatureState featureState) {
    try {
      return mapper.writeValueAsString(featureState);
    } catch (JsonProcessingException e) {
      log.error("Cannot encode feature state `{}`", featureState.toString(), e);
      throw new RuntimeException("Unable to encode feature to json", e);
    }
  }

  /**
   * Sends a signal for states that we are storing but have been removed, and then
   * (expecting featureNames to be all current features) notifies of the state of all new features.
   *
   * @param featureNames
   */
  protected void loadFeatures(Set<String> featureNames) {
    log.info("loading features base on feature set");
    // deal with the now deleted states
    Map<String, FeatureState> deletedStates = new HashMap<>(states);

    featureNames.forEach(deletedStates::remove);

    deletedStates.forEach((name, state) -> {
      log.info("found deleted feature `{}`, removing and signalling.", name);

      states.remove(name);  // take that state out as no-one cares anymore.

      signalListeners(new WatchSignal(name, null, true));
    });

    // deal with the newly created states, the start up of whatever container did this will have ensured there
    // is state for these newly minted feature states before we get the notification that these have changed.
    // so we are all good with getting their state

    // if this is our first time (i.e. we just started) then this will load all states and notify them. For each
    // loaded state it stores it in "states". clients should not call "getFeatures" as they may not all be loaded.

    featureNames.forEach(fn -> {
      if (states.get(fn) == null) {
        kvGet(fnOffset(fn)).thenApply(resp -> {
          if (!resp.getKvs().isEmpty()) {
            loadAndSignalFeatureStateChange(fn, resp.getKvs().get(0).getValue().toStringUtf8());
          }

          return resp;
        });
      }
    });

  }

  private void loadFeatures(String value) {
    Set<String> featureNames = featureNamesFromJson(value);

    loadFeatures(featureNames);
    watchForFeatureChanges(featureNames);
  }

  /**
   * tell everyone who cares about a state related to this feature
   *
   * @param watchSignal - the signal to send.
   */
  private void signalListeners(WatchSignal watchSignal) {
    signalListener.forEach(sl -> {
      sl.accept(watchSignal);
    });
  }

  private void watchFeatureNamesChange() {
    Watch.Watcher watching = watchClient.watch(new ByteSequence(offset));

    try {
      WatchResponse response = watching.listen();

      response.getEvents().forEach(e -> {
        if (e.getEventType() == WatchEvent.EventType.PUT) {
          loadFeatures(e.getKeyValue().getValue().toStringUtf8()); // reload the features
        }
      });

      watchPool.submit(this::watchFeatureNamesChange);
    } catch (InterruptedException e) {
      log.error("Failed to listen to {} - trying again.", offset);
      watchPool.submit(this::watchFeatureNamesChange);
    } catch (ClosedWatcherException cwe) {
      log.warn("pool shut down, watcher for features shut down.");
    } catch (Exception e) {
      log.warn("Other error occured, not re-listening.");
    }
  }

  private void loadAndSignalFeatureStateChange(String featureName, String json) {
    FeatureState fState = featureStateFromJson(json);

    log.info("storing state of feature `{}`", featureName);

    states.put(featureName, fState);

    signalListeners(new WatchSignal(featureName, fState, false));
  }

  private void watchFeatureStateChange(String featureName) {
    Watch.Watcher watching = watchClient.watch(new ByteSequence(fnOffset(featureName)));

    inflightWatchers.add(watching);

    try {
      WatchResponse response = watching.listen();

      log.info("received state change for feature `{}`", featureName);

      response.getEvents().forEach(e -> {
        if (e.getEventType() == WatchEvent.EventType.PUT) {
          loadAndSignalFeatureStateChange(featureName, e.getKeyValue().getValue().toStringUtf8());
        }
      });

      // TODO: deal with deleted feature state, don't re-watch it. Not high priority because it is expected
      // this service will die.
      watchPool.submit(() -> {
        watchFeatureStateChange(featureName);
      });
    } catch (InterruptedException e) {
      log.error("Failed to listen to {} - trying again.", offset);
    } catch (ClosedWatcherException cwe) {
      log.warn("feature name `{}` watch shut down, presumably due to feature reload", featureName);
    } catch (Exception e) {
      log.warn("Other error occured, not re-listening.");
    }
  }

  private CompletableFuture<PutResponse> rewriteMasterFeatureList(Set<String> featureSet) {
    String features;

    try {
      features = mapper.writeValueAsString(featureSet);
    } catch (JsonProcessingException e) {
      log.error("Unable to write master list of features");
      throw new RuntimeException(e);
    }

    log.info("writing master list of features: {}", features);

    return kvPut(offset, features);
  }

  private String jsonFeature(String name, FeatureSourceStatus status) {
    try {
      return mapper.writeValueAsString(new FeatureState(name,
        status == FeatureSourceStatus.ENABLED ? LocalDateTime.now() : null,
        status == FeatureSourceStatus.LOCKED));
    } catch (JsonProcessingException e) {
      log.error("Failed to create locked feature", e);
      throw new RuntimeException(e);
    }
  }

  private void saveNewFeatures(Map<String, FeatureSourceStatus> foundFeatures) {
    List<CompletableFuture<PutResponse>> responses = new ArrayList<>();

    log.info("saving new features with default statuses");

    // these are all "new" so they are "locked"
    foundFeatures.forEach((featureName, status) -> {
      log.info("writing new feature {}: {}", featureName, status.name());

      CompletableFuture<PutResponse> re = kvPut(fnOffset(featureName), jsonFeature(featureName, status));

      responses.add(re);
    });

    // wait for them to finish
    CompletableFuture.allOf(responses.toArray(new CompletableFuture[0]));

    log.info("new features saved");
  }

  @Override
  public void watch(Consumer<WatchSignal> changed) {
    signalListener.add(changed);
  }

  @Override
  public Map<String, FeatureState> getFeatures() {
    return ImmutableMap.copyOf(states);
  }

  @Override
  public FeatureState getFeature(String name) {
    FeatureState found = states.get(name);

    return found == null ? null : new FeatureState(found);
  }

  @Override
  public void refresh() {
    // no-op - etcd is watched
  }

  @Override
  public void apply(FeatureState featureState) {
    log.debug("applying feature state: {}", featureState.toString());
    kvPut(fnOffset(featureState.getName()), featureStateToJson(featureState));
  }
}
