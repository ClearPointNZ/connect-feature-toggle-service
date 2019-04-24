package cd.connect.features.sql;

import com.bluetrainsoftware.common.config.ConfigKey;
import io.ebean.EbeanServer;
import net.stickycode.stereotype.configured.PostConfigured;

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
  private EbeanHolder ebeanServer;


  @PostConfigured
  public void init() {
    ebeanServer = new EbeanHolder(dbUrl, dbUsername, dbPassword, maxConnections, dbDriver);
  }

  public EbeanServer getEbeanServer() {
    return ebeanServer.getEbeanServer();
  }
}
