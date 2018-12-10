package cd.connect.features.sql;

import com.bluetrainsoftware.common.config.ConfigKey;
import com.bluetrainsoftware.common.config.PreStart;
import net.stickycode.stereotype.configured.PostConfigured;

import javax.inject.Inject;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureDbSqlConfigured {
  @ConfigKey("mysql.refresh-period-in-seconds")
  Integer refreshPeriod;
  private FeatureSqlDb sqlDb;
  private final EbeanSource ebeanServer;

  @Inject
  public FeatureDbSqlConfigured(EbeanSource ebeanServer) {
    this.ebeanServer = ebeanServer;
  }

  public FeatureSqlDb getSqlDb() {
    return sqlDb;
  }

  @PostConfigured
  public void initPolling() {
    sqlDb = new FeatureSqlDb(ebeanServer, refreshPeriod);
  }

  @PreStart
  public void init() {
    sqlDb.init();
  }
}
