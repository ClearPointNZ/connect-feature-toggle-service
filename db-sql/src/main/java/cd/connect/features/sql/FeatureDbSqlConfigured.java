package cd.connect.features.sql;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;

import javax.inject.Inject;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureDbSqlConfigured {
  @ConfigKey("mysql.refresh-period-in-seconds")
  Integer refreshPeriod;
  private FeatureSqlDb sqlDb;

  @Inject
  public FeatureDbSqlConfigured(EbeanSource ebeanServer) {
    DeclaredConfigResolver.resolve(this);

    sqlDb = new FeatureSqlDb(ebeanServer, refreshPeriod);
    sqlDb.init();
  }

  public FeatureSqlDb getSqlDb() {
    return sqlDb;
  }
}
