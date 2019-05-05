package cd.connect.features.inmemory;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.features.api.FeatureDb;
import cd.connect.features.init.FeatureSource;
import cd.connect.features.resource.jersey.FeatureBinder;
import cd.connect.features.resource.jersey.FeatureServiceFeature;
import cd.connect.jersey.common.CommonConfiguration;
import cd.connect.jersey.common.InfrastructureConfiguration;
import cd.connect.jersey.common.LoggingConfiguration;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.TimeUnit;

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
      .register(InfrastructureConfiguration.class)
      .register(CommonConfiguration.class)
      .register(LoggingConfiguration.class)
      ;

    new FeatureSource(db, enumSource, listSource);

    final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, config, true);

    ApplicationLifecycleManager.registerListener(trans -> {
      if (trans.next == LifecycleStatus.TERMINATING) {
        server.shutdown(10, TimeUnit.SECONDS);
      }
    });

    ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTED);

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
