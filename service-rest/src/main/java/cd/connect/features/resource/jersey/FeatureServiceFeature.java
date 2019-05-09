package cd.connect.features.resource.jersey;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

public class FeatureServiceFeature implements Feature {
  @Override
  public boolean configure(FeatureContext featureContext) {

    featureContext.register(FeatureResource.class);
    return true;
  }
}
