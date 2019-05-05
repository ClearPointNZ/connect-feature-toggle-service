package cd.connect.features.resource.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class GrpcServerConfigured {
	private static final Logger log = LoggerFactory.getLogger(GrpcServerConfigured.class);
	private final FeatureRpcResource featureRpcResource;
	private final int port;
	private Server server;

	public GrpcServerConfigured(FeatureRpcResource featureRpcResource, int port) {
		this.featureRpcResource = featureRpcResource;
		this.port = port;
	}

  /**
   * Initializes the gRPC server and returns a shutdown hook that should be called to shut down
   * the server.
   * @return
   * @throws IOException
   */
	public Runnable init() throws IOException {
		log.info("starting grpc server on port {}", port);

		server = ServerBuilder.forPort(port)
			.addService(featureRpcResource).build().start();

		// this should be done as part of the lifecycle hook
//		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//		}));

		return () -> {
      log.info("Shutting down grpc server on port {}", port);
      server.shutdown();
    };
	}
}
