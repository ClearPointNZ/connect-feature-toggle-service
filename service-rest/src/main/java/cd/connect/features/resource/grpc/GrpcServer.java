package cd.connect.features.resource.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Standalone if wrapping in your own startup mechanism.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class GrpcServer {
	private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);
	private final Server server;
	
	public GrpcServer(FeatureRpcResource featureRpcResource, int port) throws IOException {

		log.info("starting grpc server on port {}", port);

		server = ServerBuilder.forPort(port)
			.addService(featureRpcResource).build().start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("Shutting down grpc server on port {}" + port);
			server.shutdown();
		}));
	}
}
