package cd.connect.features.resource.grpc;

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
public class GrpcServerConfigured {
	private static final Logger log = LoggerFactory.getLogger(GrpcServerConfigured.class);
	private final FeatureRpcResource featureRpcResource;
	@ConfigKey("grpc.port")
	Integer port = 2865;
	private Server server;

	public GrpcServerConfigured(FeatureRpcResource featureRpcResource) {
		this.featureRpcResource = featureRpcResource;
	}

	@PostConfigured
	public void init() throws IOException {
		log.info("starting grpc server on port {}", port);

		server = ServerBuilder.forPort(port)
			.addService(featureRpcResource).build().start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("Shutting down grpc server on port {}" + port);
			server.shutdown();
		}));
	}
}
