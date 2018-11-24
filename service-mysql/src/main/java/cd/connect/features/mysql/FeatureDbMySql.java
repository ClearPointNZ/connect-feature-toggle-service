package cd.connect.features.mysql;

import cd.connect.features.api.FeatureSourceStatus;
import cd.connect.features.api.FeatureState;
import cd.connect.features.db.FeatureDb;
import cd.connect.features.sql.EbeanHolder;
import cd.connect.features.sql.SqlFeatureState;
import com.bluetrainsoftware.common.config.ConfigKey;
import com.bluetrainsoftware.common.config.PreStart;
import io.ebean.EbeanServer;
import io.ebean.annotation.Transactional;
import net.stickycode.stereotype.configured.PostConfigured;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureDbMySql implements FeatureDb {
  private static final Logger log = LoggerFactory.getLogger(FeatureDbMySql.class);
  private final EbeanHolder ebeanHolder;
  private final List<Consumer<WatchSignal>> inflightWatchers = new ArrayList<>();
  private final ExecutorService watchPool = Executors.newCachedThreadPool();
  @ConfigKey("mysql.refresh-period-in-seconds")
  Integer refreshPeriod;
  private Map<String, SqlFeatureState> states = new HashMap<>();

  @Inject
  public FeatureDbMySql(EbeanHolder ebeanServer) {
    this.ebeanHolder = ebeanServer;
  }

  @PostConfigured
  public void initPolling() {
    if (refreshPeriod > 0) {
      watchPool.submit(this::waitAndTriggerPoll);

      Runtime.getRuntime().addShutdownHook(new Thread(watchPool::shutdown));
    }
  }

  protected void pollForUpdates() {
    log.debug("polling for updates...");

    Map<String, SqlFeatureState> newStates = new HashMap<>(states);

    ebeanHolder.getEbeanServer().find(SqlFeatureState.class).findEach(fs -> {
      SqlFeatureState existing = newStates.get(fs.getName());

      if (existing == null || !existing.equals(fs)) { // new one
        signalWatchers(new WatchSignal(fs.getName(), fs.toFeatureState(), false));
        states.put(fs.getName(), fs); // add it in
      }

      newStates.remove(fs.getName());
    });

    newStates.keySet().forEach(deletedKeys -> {
      signalWatchers(new WatchSignal(deletedKeys, newStates.get(deletedKeys).toFeatureState(), true));
      states.remove(deletedKeys);
    });

    waitAndTriggerPoll();
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
  @Transactional
  public void ensureExists(Map<String, FeatureSourceStatus> features) {
    EbeanServer ebeanServer = ebeanHolder.getEbeanServer();

    features.entrySet().forEach((entry) -> {
      SqlFeatureState featureState = ebeanServer.find(SqlFeatureState.class, entry.getKey());

      if (featureState == null) {
        featureState = new SqlFeatureState(entry.getKey(),
          entry.getValue() == FeatureSourceStatus.ENABLED ? LocalDateTime.now() : null,
          entry.getValue() == FeatureSourceStatus.LOCKED);

        ebeanServer.save(featureState);
      } else {
        if (entry.getValue() == FeatureSourceStatus.ENABLED) {
          if (featureState.getWhenEnabled() == null) {
            featureState.setWhenEnabled(LocalDateTime.now());
          }
        } else {
          featureState.setWhenEnabled(null);
        }

        featureState.setLocked(entry.getValue() == FeatureSourceStatus.LOCKED);

        ebeanServer.update(featureState);
      }
    });
  }

  @Override
  public void watch(Consumer<WatchSignal> changed) {
    if (inflightWatchers.size() == 0) {
      loadStateForWatchers();
    }

    inflightWatchers.add(changed);
  }

  private void loadStateForWatchers() {
    ebeanHolder.getEbeanServer().find(SqlFeatureState.class).findEach(fs -> states.put(fs.getName(), fs));
  }


  @Override
  public Map<String, FeatureState> getFeatures() {
    EbeanServer ebeanServer = ebeanHolder.getEbeanServer();

    Map<String, FeatureState> states = new HashMap<>();

    ebeanServer.find(SqlFeatureState.class).findEach(fs -> {
      states.put(fs.getName(), fs.toFeatureState());
    });

    return states;
  }

  @Override
  public FeatureState getFeature(String name) {
    EbeanServer ebeanServer = ebeanHolder.getEbeanServer();

    SqlFeatureState fs = ebeanServer.find(SqlFeatureState.class, name);

    return fs == null ? null : fs.toFeatureState();
  }

  @Override
  public void refresh() {
  }

  @Override
  @Transactional
  public void apply(FeatureState featureState) {
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
  @PreStart
  public void init() {
    log.info("prestart in feature-db-mysql");
  }
}
