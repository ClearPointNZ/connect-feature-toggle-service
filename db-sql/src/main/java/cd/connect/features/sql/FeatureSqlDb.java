package cd.connect.features.sql;

import cd.connect.features.api.FeatureDb;
import cd.connect.features.api.FeatureSourceStatus;
import cd.connect.features.api.FeatureState;
import io.ebean.EbeanServer;
import io.ebean.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureSqlDb implements FeatureDb {
  private static final Logger log = LoggerFactory.getLogger(FeatureSqlDb.class);
  private final EbeanSource ebeanHolder;
  private final List<Consumer<FeatureDb.WatchSignal>> inflightWatchers = new ArrayList<>();
  private final ExecutorService watchPool = Executors.newCachedThreadPool();
  private final int refreshPeriod;
  private Map<String, SqlFeatureState> states = new HashMap<>();

  public FeatureSqlDb(EbeanSource ebeanServer, int refreshPeriod) {
    this.ebeanHolder = ebeanServer;
    this.refreshPeriod = refreshPeriod;
    
    if (refreshPeriod > 0) {
      loadStateForWatchers();

      watchPool.submit(this::waitAndTriggerPoll);

      Runtime.getRuntime().addShutdownHook(new Thread(watchPool::shutdown));
    } else {
      log.info("no refresh time, always loading from db.");
    }
  }

  protected void pollForUpdates() {
    refreshStates();

    waitAndTriggerPoll();
  }

  private void refreshStates() {
    log.debug("refreshing states from database...");

    Map<String, SqlFeatureState> newStates = new HashMap<>(states);

    ebeanHolder.getEbeanServer().find(SqlFeatureState.class).findEach(fs -> {
      SqlFeatureState existing = newStates.get(fs.getName());

      if (existing == null || !existing.equals(fs)) { // new one
        log.info("state `{}` updated, changing and signalling watchers", fs.getName());
        states.put(fs.getName(), fs); // add it in
        signalWatchers(new WatchSignal(fs.getName(), fs.toFeatureState(), false));
      }

      newStates.remove(fs.getName());
    });

    newStates.keySet().forEach(deletedKeys -> {
      signalWatchers(new WatchSignal(deletedKeys, newStates.get(deletedKeys).toFeatureState(), true));
      states.remove(deletedKeys);
    });
  }

  private void waitAndTriggerPoll() {
    try {
      Thread.sleep(refreshPeriod * 1000);
      watchPool.submit(this::pollForUpdates); // put ourselves back in the running.
    } catch (InterruptedException e) {
      log.warn("Stopping polling as thread was interrupted.");
    }
  }

  private void signalWatchers(WatchSignal watchSignal) {
    log.debug("signaling a change in {}", watchSignal);

    inflightWatchers.forEach(c -> {
      watchPool.submit(() -> {
        c.accept(watchSignal);
      });
    });
  }

  @Override
  public void ensureExists(Map<String, FeatureSourceStatus> features) {
    EbeanServer ebeanServer = ebeanHolder.getEbeanServer();

    features.forEach((key, value) -> {
      SqlFeatureState featureState = ebeanServer.find(SqlFeatureState.class, key);

      if (featureState == null) {
        featureState = new SqlFeatureState(key,
          value == FeatureSourceStatus.ENABLED ? LocalDateTime.now() : null,
          value == FeatureSourceStatus.LOCKED);

        saveFeatureState(featureState);
      }
      // else {
      // it is there, leave it alone
      //}
    });

    List<SqlFeatureState> deleteItems = new ArrayList<>();
    ebeanServer.find(SqlFeatureState.class).findEach(e -> {
      if (features.get(e.getName()) == null) {
        deleteItems.add(e);
      }
    });

  }

  @Transactional
  private void batchDelete(Collection<SqlFeatureState> deleteItems) {
    EbeanServer ebeanServer = ebeanHolder.getEbeanServer();

    if (deleteItems.size() > 0) {
      deleteItems.forEach(ebeanServer::delete);
    }
  }

  @Override
  public void ensureExists(List<String> features) {
    EbeanServer ebeanServer = ebeanHolder.getEbeanServer();

    features.forEach(f -> {
      if(ebeanServer.find(SqlFeatureState.class, f) == null) {
        try {
          saveFeatureState(new SqlFeatureState(f, null, true));
        } catch (Exception e) {
          log.error("Failed to save feature", e);
        }
      }
    });
  }

  @Transactional
  private void saveFeatureState(SqlFeatureState fs) {
    ebeanHolder.getEbeanServer().save(fs);
  }

  @Override
  public void watch(Consumer<WatchSignal> changed) {
    if (inflightWatchers.size() == 0) {
      loadStateForWatchers();
    }

    inflightWatchers.add(changed);
  }

  private void loadStateForWatchers() {
    log.info("loading initial state from db");
    ebeanHolder.getEbeanServer().find(SqlFeatureState.class).findEach(fs -> states.put(fs.getName(), fs));
  }


  @Override
  public Map<String, FeatureState> getFeatures() {
    final Map<String, FeatureState> states = new HashMap<>();

    if (refreshPeriod > 0) {
      this.states.forEach((k, v) -> states.put(k, v.toFeatureState()));
    } else {
      EbeanServer ebeanServer = ebeanHolder.getEbeanServer();

      ebeanServer.find(SqlFeatureState.class).findEach(fs -> {
        states.put(fs.getName(), fs.toFeatureState());
      });
    }

    return states;
  }

  @Override
  public FeatureState getFeature(String name) {
    SqlFeatureState fs = null;

    if (refreshPeriod > 0) {
      fs = states.get(name);
    } else {
      EbeanServer ebeanServer = ebeanHolder.getEbeanServer();

      fs = ebeanServer.find(SqlFeatureState.class, name);
    }

    return fs == null ? null : fs.toFeatureState();
  }

  @Override
  public void refresh() {
  }

  @Transactional
  private void applyFeatureChange(FeatureState featureState) {
    EbeanServer ebeanServer = ebeanHolder.getEbeanServer();

    SqlFeatureState fs = ebeanServer.find(SqlFeatureState.class, featureState.getName());
    if (fs == null) {
      ebeanServer.save(SqlFeatureState.fromFeatureState(featureState));
    } else {
      fs.setWhenEnabled(featureState.getWhenEnabled());
      fs.setLocked(featureState.isLocked());
      ebeanServer.save(fs);
    }
  }

  @Override
  public void apply(FeatureState featureState) {
    applyFeatureChange(featureState);
    
    if (refreshPeriod > 0) {
      refreshStates();
    }
  }

  @Override
  public void init() {
    log.info("prestart in feature-db-mysql");
  }
}
