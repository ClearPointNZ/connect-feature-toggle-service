package cd.connect.features.resource;

import cd.connect.features.api.FeatureState;
import cd.connect.features.db.FeatureDb;
import cd.connect.features.grpc.FeatureServiceGrpc;
import cd.connect.features.grpc.FeatureStateService;
import cd.connect.features.services.BadStateException;
import cd.connect.features.services.FeatureStateChangeService;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import static cd.connect.features.grpc.FeatureStateService.*;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureRpcResource extends FeatureServiceGrpc.FeatureServiceImplBase {
	private static final Logger log = LoggerFactory.getLogger(FeatureRpcResource.class);

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
	  FeatureStateService.FeatureState.Builder builder = FeatureStateService.FeatureState.newBuilder()
		  .setName(fs.getName())
		  .setLocked(fs.isLocked());

	  if (fs.getWhenEnabled() != null) {
	  	builder.setWhenEnabled(localDateTimeToString(fs.getWhenEnabled()));
	  }

	   return builder.build();
  }

  private FeatureState toFeatureState(FeatureStateService.FeatureState state) {
  	return new FeatureState(state.getName(), stringToLocalDateTime(state.getWhenEnabled()), state.getLocked());
  }

  @Override
  public void allFeatures(Empty request, StreamObserver<FeatureStates> resp) {
	  FeatureStates.Builder fsBuilder = FeatureStates.newBuilder();
	  featureDb.getFeatures().forEach((name, fs) -> {
	  	log.info("feature is {}", fs);
	  	fsBuilder.addStates(fromFeatureState(fs));
	  });
	  resp.onNext(fsBuilder.build());
  }

  @Override
  public void applyAll(FeatureStates request, StreamObserver<FeatureStates> resp) {
	  FeatureStates.Builder fsBuilder = FeatureStates.newBuilder();

	  request.getStatesList().forEach(fs -> {
  		featureDb.apply(toFeatureState(fs));
  		fsBuilder.addStates(fs);
	  });

  	resp.onNext(fsBuilder.build());
  }

  @Override
  public void refresh(Empty request, StreamObserver<Result> resp) {
    featureDb.refresh();
    resp.onNext(Result.newBuilder().build());
  }

  @Override
  public void featureCount(Empty request, StreamObserver<FeatureCount> resp) {
	  resp.onNext(FeatureCount.newBuilder().setCount(featureDb.getFeatures().size()).build());
  }

  @Override
  public void enabledCount(Empty request, StreamObserver<FeatureCount> resp) {
  	long count = featureDb.getFeatures().values().stream().filter(FeatureState::isEnabled).count();
    resp.onNext(FeatureCount.newBuilder().setCount((int)count).build());
  }

  @Override
  public void enableAll(Empty request, StreamObserver<Result> resp) {
	  Result.Builder status = Result.newBuilder().setStatus(Result.Status.OK);

	  featureDb.getFeatures().values().forEach(f -> {
		  try {
			  featureStateChangeService.enable(f);

		  } catch (BadStateException e) {
			  log.warn("Enable failed", e);
			  status.setStatus(Result.Status.ALREADY_LOCKED);
		  }
	  });

	  resp.onNext(status.build());
  }

  @Override
  public void delete(FeatureName request, StreamObserver<Result> resp) {
	  resp.onNext(Result.newBuilder().setStatus(Result.Status.CANNOT_DELETE).build());
  }

  @Override
  public void enable(FeatureName request, StreamObserver<Result> resp) {
	  FeatureState feature = featureDb.getFeature(request.getName());
	  if (feature != null) {
		  try {
			  featureStateChangeService.enable(feature);
			  resp.onNext(Result.newBuilder().setStatus(Result.Status.OK).build());
		  } catch (BadStateException e) {
			  log.warn("Enabled failed", e);
			  resp.onNext(Result.newBuilder().setStatus(Result.Status.ALREADY_LOCKED).build());
		  }
	  }
  }

  @Override
  public void disable(FeatureName request, StreamObserver<Result> resp) {
	  FeatureState feature = featureDb.getFeature(request.getName());
	  if (feature != null) {
		  try {
			  featureStateChangeService.disable(feature);
			  resp.onNext(Result.newBuilder().setStatus(Result.Status.OK).build());
		  } catch (BadStateException e) {
			  log.warn("Disabled failed", e);
			  resp.onNext(Result.newBuilder().setStatus(Result.Status.ALREADY_LOCKED).build());
		  }
	  }
  }

  @Override
  public void lock(FeatureName request, StreamObserver<Result> resp) {
	  FeatureState feature = featureDb.getFeature(request.getName());
	  if (feature != null) {
		  try {
			  featureStateChangeService.lock(feature);
			  resp.onNext(Result.newBuilder().setStatus(Result.Status.OK).build());
		  } catch (BadStateException e) {
			  log.warn("Lock failed", e);
			  resp.onNext(Result.newBuilder().setStatus(Result.Status.ALREADY_ENABLED).build());
		  }
	  }
  }

  @Override
  public void unlock(FeatureName request, StreamObserver<Result> resp) {
	  FeatureState feature = featureDb.getFeature(request.getName());
	  if (feature != null) {
		  try {
			  featureStateChangeService.unlock(feature);
			  resp.onNext(Result.newBuilder().setStatus(Result.Status.OK).build());
		  } catch (BadStateException e) {
			  log.warn("Unlock failed", e);
			  resp.onNext(Result.newBuilder().setStatus(Result.Status.ALREADY_ENABLED).build());
		  }
	  }
  }

  @Override
  public void watch(Empty request, StreamObserver<FeatureStateService.FeatureState> resp) {
	  featureDb.getFeatures().forEach((name, fs) -> {
		  FeatureStateService.FeatureState.Builder builder = FeatureStateService.FeatureState.newBuilder()
			  .setLocked(fs.isLocked())
			  .setName(name)
			  .setDeleted(false);

		  if (fs.getWhenEnabled() != null) {
		  	builder.setWhenEnabled(localDateTimeToString(fs.getWhenEnabled()));
		  }

		  resp.onNext(builder.build());
	  });

    featureDb.watch(ws -> {
    	try {
		    resp.onNext(FeatureStateService.FeatureState.newBuilder()
			    .setLocked(ws.getState().isLocked())
			    .setName(ws.getName())
			    .setWhenEnabled(ws.getState().getWhenEnabled().toString())
			    .setDeleted(ws.isDeleted())
			    .build());
	    } catch (Throwable t) {
    		log.debug("Failed to send response on watch");
	    }
    });
  }
}
