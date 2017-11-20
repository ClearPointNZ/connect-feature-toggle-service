package cd.connect.features.resource;

import com.bluetrainsoftware.common.config.ConfigKey;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import net.stickycode.stereotype.configured.PostConfigured;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class GrpcServer {
  private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);
  
  @ConfigKey("grpc.port")
  Integer port = 2865;

  private Server server;

  private final FeatureRpcResource featureRpcResource;

  public GrpcServer(FeatureRpcResource featureRpcResource) {
    this.featureRpcResource = featureRpcResource;
  }

  @PostConfigured
  public void init() throws IOException {
    log.info("starting grpc server on port {}", port);
    
    server = ServerBuilder.forPort(port).addService(featureRpcResource).build().start();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Shutting down grpc server on port " + port);
      server.shutdown();
    }));
  }
}
