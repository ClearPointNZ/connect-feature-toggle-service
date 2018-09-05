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
import java.util.function.Consumer;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureDbMySql implements FeatureDb {
  private static final Logger log = LoggerFactory.getLogger(FeatureDbMySql.class);

	@ConfigKey("mysql.refresh-period-in-seconds")
	Integer refreshPeriod;

	private final EbeanHolder ebeanHolder;
  private final List<Consumer<WatchSignal>> inflightWatchers = new ArrayList<>();

	@Inject
  public FeatureDbMySql(EbeanHolder ebeanServer) {
    this.ebeanHolder = ebeanServer;
  }

  @Override
  @Transactional
	public void ensureExists(Map<String, FeatureSourceStatus> features) {
	  EbeanServer ebeanServer = ebeanHolder.getEbeanServer();

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
