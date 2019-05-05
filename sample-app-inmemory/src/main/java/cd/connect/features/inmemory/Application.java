package cd.connect.features.inmemory;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.features.api.FeatureDb;
import cd.connect.features.init.FeatureSource;
import cd.connect.features.resource.jersey.FeatureBinder;
import cd.connect.features.resource.jersey.FeatureResource;
import cd.connect.features.resource.jersey.FeatureServiceFeature;
import cd.connect.features.services.FeatureStateChangeService;
import cd.connect.jersey.common.CommonConfiguration;
import cd.connect.jersey.common.InfrastructureConfiguration;
import cd.connect.jersey.common.JacksonContextProvider;
import cd.connect.jersey.common.JerseyExceptionMapper;
import cd.connect.jersey.common.LoggingConfiguration;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.netty.channel.Channel;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.logging.JerseyServerLogger;
import org.glassfish.jersey.netty.httpserver.NettyHttpContainerProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.net.URI;

public class Application {
  @ConfigKey("enumSource")
  String enumSource = "";
  @ConfigKey("listSource")
  String listSource = "";
  @ConfigKey("server.port")
  String serverPort = "8099";

  private static final Logger log = LoggerFactory.getLogger(Application.class);

  public void run() throws Exception {
    ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTING);

    DeclaredConfigResolver.resolve(this);

    URI BASE_URI = URI.create(String.format("http://localhost:%s/", serverPort));

    log.info("starting on port {}", BASE_URI.toASCIIString());

    FeatureDb db = new InMemoryDb();

    ResourceConfig config = new ResourceConfig()
      .register(FeatureServiceFeature.class)
      .register(new FeatureBinder(db))
      .register(JacksonContextProvider.class)
      .register(JerseyExceptionMapper.class)
      .register(InfrastructureConfiguration.class)
      .register(CommonConfiguration.class)
      .register(LoggingConfiguration.class)
      ;

    new FeatureSource(db, enumSource, listSource);

    Channel server = NettyHttpContainerProvider.createHttp2Server(BASE_URI, config, null);

    ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTED);

    ApplicationLifecycleManager.registerListener(trans -> {
      if (trans.next == LifecycleStatus.TERMINATING) {
        server.close();
      }
    });

    Thread.currentThread().join();
  }

  public static void main(String[] args) {
    try {
      new Application().run();
    } catch (Exception e) {
      log.error("Failed to start.");
      System.exit(-1);
    }
  }
}
