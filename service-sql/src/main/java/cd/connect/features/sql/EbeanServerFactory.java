package cd.connect.features.sql;

import com.bluetrainsoftware.common.config.ConfigKey;
import io.ebean.EbeanServer;
import io.ebean.config.ServerConfig;
import org.avaje.datasource.DataSourceConfig;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class EbeanServerFactory implements FactoryBean<EbeanServer> {

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

	@Override
	public EbeanServer getObject() throws Exception {
		return createEbeanServer();
	}

	private EbeanServer createEbeanServer() {
		ServerConfig config = new ServerConfig();

		DataSourceConfig dsConfig = new DataSourceConfig();

		dsConfig.setUrl(dbUrl);
		dsConfig.setUsername(dbUsername);
		dsConfig.setPassword(dbPassword);
		dsConfig.setDriver(dbDriver);
		dsConfig.setMaxConnections(maxConnections);

		config.setDataSourceConfig(dsConfig);

		config.setDdlRun(true);

		return io.ebean.EbeanServerFactory.create(config);
	}

	@Override
	public Class<?> getObjectType() {
		return EbeanServer.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
