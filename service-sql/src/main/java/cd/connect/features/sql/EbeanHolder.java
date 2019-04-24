package cd.connect.features.sql;

import io.ebean.EbeanServer;
import io.ebean.annotation.Platform;
import io.ebean.config.DbMigrationConfig;
import io.ebean.config.ServerConfig;
import io.ebean.datasource.DataSourceConfig;

import javax.sql.DataSource;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class EbeanHolder implements EbeanSource {
  private final EbeanServer ebeanServer;
  private final ServerConfig config;

  public EbeanHolder(String dbUrl, String dbUsername, String dbPassword, int maxConnections, String dbDriver) {
    this.config = new ServerConfig();

    DataSourceConfig dsConfig = new DataSourceConfig();

    dsConfig.setUrl(dbUrl);
    dsConfig.setUsername(dbUsername);
    dsConfig.setPassword(dbPassword);
    dsConfig.setDriver(dbDriver);
    dsConfig.setMaxConnections(maxConnections);

    config.setDataSourceConfig(dsConfig);

    DbMigrationConfig migrationConfig = new DbMigrationConfig();
    migrationConfig.setPlatform(Platform.MYSQL);
    migrationConfig.setRunMigration(true);
    config.setMigrationConfig(migrationConfig);

    this.ebeanServer = io.ebean.EbeanServerFactory.create(config);
  }

  @Override
  public EbeanServer getEbeanServer() {
    return ebeanServer;
  }

  @Override
  public DataSource getDatasource() {
    return config.getDataSource();
  }
}
