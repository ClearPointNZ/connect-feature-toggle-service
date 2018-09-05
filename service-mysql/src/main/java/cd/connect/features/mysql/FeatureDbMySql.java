package cd.connect.features.mysql;

import cd.connect.features.api.FeatureSourceStatus;
import cd.connect.features.api.FeatureState;
import cd.connect.features.db.FeatureDb;
import cd.connect.features.sql.SqlFeatureState;
import com.bluetrainsoftware.common.config.ConfigKey;
import io.ebean.EbeanServer;
import io.ebean.annotation.Transactional;
import net.stickycode.stereotype.configured.PostConfigured;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureDbMySql implements FeatureDb {
	@ConfigKey("mysql.refresh-period-in-seconds")
	Integer refreshPeriod;

	private final EbeanServer ebeanServer;
  private final List<Consumer<WatchSignal>> inflightWatchers = new ArrayList<>();

	@Inject
  public FeatureDbMySql(EbeanServer ebeanServer) {
    this.ebeanServer = ebeanServer;
  }

  @Override
  @Transactional
	public void ensureExists(Map<String, FeatureSourceStatus> features) {
    features.entrySet().stream().forEach((entry) -> {
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
    inflightWatchers.add(changed);
	}

	@Override
	public Map<String, FeatureState> getFeatures() {
	  Map<String, FeatureState> states = new HashMap<>();

	  ebeanServer.find(SqlFeatureState.class).findEach(fs -> {
	    states.put(fs.getName(), fs.toFeatureState());
    });

		return states;
	}

	@Override
	public FeatureState getFeature(String name) {
    SqlFeatureState fs = ebeanServer.find(SqlFeatureState.class, name);

    return fs == null ? null : fs.toFeatureState();
	}

	@Override
	public void refresh() {
	}

	@Override
  @Transactional
	public void apply(FeatureState featureState) {
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
  @PostConfigured
	public void init() {
	  // read in all feature toggles, create the ones
	}
}
