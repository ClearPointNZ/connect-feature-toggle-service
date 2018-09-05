package cd.connect.features.resource;

import cd.connect.features.api.FeatureState;
import cd.connect.features.db.FeatureDb;
import cd.connect.features.grpc.FeatureServiceGrpc;
import cd.connect.features.grpc.FeatureStateService;
import cd.connect.features.services.FeatureStateChangeService;
import io.grpc.stub.StreamObserver;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureRpcResource extends FeatureServiceGrpc.FeatureServiceImplBase {
  private final FeatureDb featureDb;
  private final FeatureStateChangeService featureStateChangeService;

  @Inject
  public FeatureRpcResource(FeatureDb featureDb, FeatureStateChangeService featureStateChangeService) {
    this.featureDb = featureDb;
    this.featureStateChangeService = featureStateChangeService;
  }

  // return ISO8601
  private String localDateTimeToString(LocalDateTime time) {
  	if (time != null) {
		  return time.atZone(ZoneOffset.UTC).toString();
	  }

	  return null;
  }

  // expect ISO8601
  private LocalDateTime stringToLocalDateTime(String str) {
  	if (str == null) {
		  return null;
	  }

	  return LocalDateTime.parse(str);
  }

  private FeatureStateService.FeatureState fromFeatureState(FeatureState fs) {
    return FeatureStateService.FeatureState.newBuilder()
	    .setName(fs.getName())
	    .setLocked(fs.isLocked())
	    .setWhenEnabled(localDateTimeToString(fs.getWhenEnabled()))
	    .build();
  }

  private FeatureState toFeatureState(FeatureStateService.FeatureState state) {
  	return new FeatureState(state.getName(), stringToLocalDateTime(state.getWhenEnabled()), state.getLocked());
  }

  @Override
  public void allFeatures(FeatureStateService.Empty request, StreamObserver<FeatureStateService.FeatureStates> resp) {
	  FeatureStateService.FeatureStates.Builder fsBuilder = FeatureStateService.FeatureStates.newBuilder();
	  featureDb.getFeatures().forEach((name, fs) -> {
	  	fsBuilder.addStates(fromFeatureState(fs));
	  });
	  resp.onNext(fsBuilder.build());
  }

  @Override
  public void applyAll(FeatureStateService.FeatureStates request, StreamObserver<FeatureStateService.FeatureStates> resp) {
    super.applyAll(request, resp);
  }

  @Override
  public void refresh(FeatureStateService.Empty request, StreamObserver<FeatureStateService.Status> resp) {
    super.refresh(request, resp);
  }

  @Override
  public void featureCount(FeatureStateService.Empty request, StreamObserver<FeatureStateService.FeatureCount> resp) {
    super.featureCount(request, resp);
  }

  @Override
  public void enabledCount(FeatureStateService.Empty request, StreamObserver<FeatureStateService.FeatureCount> resp) {
    super.enabledCount(request, resp);
  }

  @Override
  public void enableAll(FeatureStateService.Empty request, StreamObserver<FeatureStateService.Status> resp) {
    super.enableAll(request, resp);
  }

  @Override
  public void delete(FeatureStateService.FeatureName request, StreamObserver<FeatureStateService.Status> resp) {
    super.delete(request, resp);
  }

  @Override
  public void enable(FeatureStateService.FeatureName request, StreamObserver<FeatureStateService.Status> resp) {
    super.enable(request, resp);
  }

  @Override
  public void disable(FeatureStateService.FeatureName request, StreamObserver<FeatureStateService.Status> resp) {
    super.disable(request, resp);
  }

  @Override
  public void lock(FeatureStateService.FeatureName request, StreamObserver<FeatureStateService.Status> resp) {
    super.lock(request, resp);
  }

  @Override
  public void unlock(FeatureStateService.FeatureName request, StreamObserver<FeatureStateService.Status> resp) {
    super.unlock(request, resp);
  }

  @Override
  public void watch(FeatureStateService.Empty request, StreamObserver<FeatureStateService.FeatureState> resp) {
    super.watch(request, resp);
  }
}
