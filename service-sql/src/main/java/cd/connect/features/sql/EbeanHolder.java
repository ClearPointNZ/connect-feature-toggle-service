package cd.connect.features.sql;

import com.bluetrainsoftware.common.config.ConfigKey;
import io.ebean.EbeanServer;
import io.ebean.annotation.Platform;
import io.ebean.config.DbMigrationConfig;
import io.ebean.config.ServerConfig;
import net.stickycode.stereotype.configured.PostConfigured;
import org.avaje.datasource.DataSourceConfig;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class EbeanHolder {
	private EbeanServer ebeanServer;
	
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

	@PostConfigured
	public void init() {
		ServerConfig config = new ServerConfig();

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

		ebeanServer = io.ebean.EbeanServerFactory.create(config);
	}

	public EbeanServer getEbeanServer() {
		return ebeanServer;
	}
}
