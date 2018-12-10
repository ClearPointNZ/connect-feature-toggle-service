package cd.connect.features;

import cd.connect.features.db.FeatureDbEtcd;
import cd.connect.features.init.FeatureSource;
import cd.connect.features.resource.grpc.FeatureRpcResource;
import cd.connect.features.resource.grpc.GrpcServerConfigured;
import cd.connect.features.resource.jersey.JerseyModule;
import cd.connect.features.services.FeatureStateChangeService;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({FeatureSource.class, JerseyModule.class, GrpcServerConfigured.class,
  FeatureStateChangeService.class, FeatureRpcResource.class, FeatureDbEtcd.class})
public @interface EnableEtcdToggleServer {
}
