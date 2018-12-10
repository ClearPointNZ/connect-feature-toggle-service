package cd.connect.features.client;

import cd.connect.features.api.FeatureService;
import org.glassfish.jersey.client.proxy.WebResourceFactory;

import javax.ws.rs.client.Client;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class BaseRestRepository implements FeatureRepository {

  // in case extra services are required.
  protected final FeatureService featureService;

  public BaseRestRepository(Client client, String url) {
    this.featureService = WebResourceFactory.newResource(FeatureService.class, client.target(url));
  }

  /**
   * we assume locked unless we find the feature and then we honour it.
   * 
   * @param name - name of feature
   * @return - state of feature
   */
  @Override
  public FeatureState getFeatureState(String name) {
    cd.connect.features.client.FeatureState featureState = new cd.connect.features.client.FeatureState();
    featureState.name = name;

    try {
      cd.connect.features.api.FeatureState feature = featureService.getFeature(name);

      featureState.whenEnabled = feature.getWhenEnabled();
      featureState.locked = feature.isLocked();
    } catch (Exception ex) {
      featureState.locked = true;
    }

    return featureState;
  }
}
