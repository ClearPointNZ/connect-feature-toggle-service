package cd.connect.features.resource;

import cd.connect.features.api.FeatureState;
import cd.connect.features.db.FeatureDb;
import cd.connect.features.grpc.FeatureServiceGrpc;
import cd.connect.features.grpc.FeatureStateService;
import cd.connect.features.services.FeatureStateChangeService;
import io.grpc.stub.StreamObserver;

import javax.inject.Inject;
import java.time.LocalDateTime;
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

  private String localDateTimeToString(LocalDateTime time) {
	  return null;
  }

  private LocalDateTime stringToLocalDateTime(String str) {
	  return null;
  }

  private FeatureStateService.FeatureState fromFeatureState(FeatureState fs) {
//    FeatureStateService.FeatureState.newBuilder().setLocked(fs.isLocked()).setWhenEnabled()
	  return null;
  }

  private FeatureState toFeatureState(FeatureStateService.FeatureState state) {
	  return null;
  }

  @Override
  public void allFeatures(FeatureStateService.Empty request, StreamObserver<FeatureStateService.FeatureStates> resp) {
//    FeatureStateService.FeatureStates.Builder builder = FeatureStateService.FeatureStates.newBuilder();
//
//    featureDb.getFeatures().forEach((name, fs) -> {
//
//      builder.addStates(fs);
//    });
//
//    resp.onNext(new ArrayList<>(.values()));
    super.allFeatures(request, resp);
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
