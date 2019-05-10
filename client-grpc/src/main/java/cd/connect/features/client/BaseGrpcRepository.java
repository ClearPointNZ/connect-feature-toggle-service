package cd.connect.features.client;

import cd.connect.features.grpc.FeatureServiceGrpc;
import cd.connect.features.grpc.FeatureStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class BaseGrpcRepository implements FeatureRepository {
  private static final Logger log = LoggerFactory.getLogger(BaseGrpcRepository.class);
  // in case extra services are required.
  protected final FeatureServiceGrpc.FeatureServiceBlockingStub featureService;

  public BaseGrpcRepository(io.grpc.Channel channel) {
    this.featureService = FeatureServiceGrpc.newBlockingStub(channel);
  }

  private LocalDateTime stringToLocalDateTime(String str) {
    if (str == null || str.trim().length() == 0) {
      return null;
    }

    return LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME).atZone(ZoneOffset.UTC).toLocalDateTime();
  }

  /**
   * we assume locked unless we find the feature and then we honour it.
   *
   * @param name - name of feature
   * @return - state of feature
   */
  @Override
  public FeatureState getFeatureState(String name) {
    FeatureState fs = new FeatureState();
    fs.name = name;
    
    try {
      FeatureStateService.FeatureState state = featureService.state(FeatureStateService.FeatureName.newBuilder().setName(name).build());
      fs.locked = state.getLocked();
      fs.whenEnabled = stringToLocalDateTime(state.getWhenEnabled());
    } catch (Exception e) {
      fs.locked = true;
    }

    return fs;
  }

  @Override
  public void ensureFeaturesExist(Class<? extends Enum> features) {
  }
}
