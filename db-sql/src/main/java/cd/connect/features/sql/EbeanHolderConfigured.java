package cd.connect.features.sql;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.ebean.EbeanServer;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class EbeanHolderConfigured {
  @ConfigKey("db.url")
  String dbUrl;
  @ConfigKey("db.driver")
  String dbDriver = "";
  @ConfigKey("db.password")
  String dbPassword;
  @ConfigKey("db.username")
  String dbUsername;
  @ConfigKey("db.max-connections")
  Integer maxConnections = 3;
  private final EbeanHolder ebeanServer;

  public EbeanHolderConfigured() {
    DeclaredConfigResolver.resolve(this);
    ebeanServer = new EbeanHolder(dbUrl, dbUsername, dbPassword, maxConnections, dbDriver);
  }

  public EbeanServer getEbeanServer() {
    return ebeanServer.getEbeanServer();
  }
}
