package cd.connect.features.client;

import cd.connect.features.FeatureService;
import cd.connect.features.impl.FeatureServiceImpl;
import cd.connect.openapi.support.ApiClient;

import javax.ws.rs.client.Client;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class BaseRestRepository implements FeatureRepository {

  // in case extra services are required.
  protected final FeatureService featureService;

  public BaseRestRepository(Client client, String url) {
    this.featureService = new FeatureServiceImpl(new ApiClient(client, url));
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
      cd.connect.features.FeatureState feature = featureService.getFeature(name);

      featureState.whenEnabled = LocalDateTime.from(feature.getWhenEnabled());
      featureState.locked = feature.isLocked();
    } catch (Exception ex) {
      featureState.locked = true;
    }

    return featureState;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void ensureFeaturesExist(Class<? extends Enum> features) {
    List<String> names = new ArrayList<>();
    EnumSet<? extends Enum> s = EnumSet.allOf(features);
    
    for(Enum e : s) {
      names.add(e.name());
    }

    if (names.size() > 0) {
      featureService.ensureExists(names);
    }
  }
}
