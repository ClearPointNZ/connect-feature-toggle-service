package cd.connect.features;

import cd.connect.features.db.FeatureDbEtcd;
import cd.connect.features.init.FeatureSource;
import cd.connect.features.resource.FeatureRpcResource;
import cd.connect.features.resource.GrpcServer;
import cd.connect.features.services.FeatureStateChangeService;
import cd.connect.spring.jersey.BaseWebApplication;
import org.springframework.context.annotation.Import;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@Import({FeatureSource.class, JerseyModule.class, FeatureDbEtcd.class, GrpcServer.class, FeatureStateChangeService.class, FeatureRpcResource.class})
public class FeatureServiceApplication extends BaseWebApplication {
}
