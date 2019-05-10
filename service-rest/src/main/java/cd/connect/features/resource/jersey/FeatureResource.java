package cd.connect.features.resource.jersey;

import cd.connect.features.FeatureService;
import cd.connect.features.FeatureState;
import cd.connect.features.api.FeatureDb;
import cd.connect.features.services.BadStateException;
import cd.connect.features.services.FeatureStateChangeService;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@Api
public class FeatureResource implements FeatureService {
	private final FeatureDb featureDb;
	private final FeatureStateChangeService featureStateChangeService;

	@Inject
	public FeatureResource(FeatureDb featureDb, FeatureStateChangeService featureStateChangeService) {
		this.featureDb = featureDb;
		this.featureStateChangeService = featureStateChangeService;
	}

	private FeatureState from(cd.connect.features.api.FeatureState fs) {
	  return new FeatureState().name(fs.getName()).locked(fs.isLocked())
      .whenEnabled(fs.getWhenEnabled() == null ? null : fs.getWhenEnabled().atZone(ZoneOffset.systemDefault()).toOffsetDateTime());
  }

  private cd.connect.features.api.FeatureState to(FeatureState fs) {
	  return new cd.connect.features.api.FeatureState(fs.getName(),
      fs.getWhenEnabled() == null ? null : LocalDateTime.from(fs.getWhenEnabled()),
      fs.isLocked());
  }

  private List<FeatureState> from(List<cd.connect.features.api.FeatureState> fs) {
	  return fs.stream().map(this::from).collect(Collectors.toList());
  }

  private List<cd.connect.features.api.FeatureState> to(List<FeatureState> fs) {
	  return fs.stream().map(this::to).collect(Collectors.toList());
  }

	@Override
	public FeatureState getFeature(String feature_name) {
		FeatureState fs = from(featureDb.getFeature(feature_name));

		if (fs == null) {
			throw new NotFoundException("No such feature");
		}

		return fs;
	}

	@Override
	public List<FeatureState> allFeatures() {
		return new ArrayList<>(from(new ArrayList<>(featureDb.getFeatures().values())));
	}

	@Override
	public List<FeatureState> applyAll(List<FeatureState> entries) {
		Map<String, cd.connect.features.api.FeatureState> states = featureDb.getFeatures();

    List<cd.connect.features.api.FeatureState> internalFeatureEntries = to(entries);

		try {
      featureStateChangeService.validStateCheck(internalFeatureEntries);
		} catch (BadStateException e) {
			throw new BadRequestException(e.getMessage());
		}

		long missing = entries.stream().filter(s -> s.getName() == null || states.get(s.getName()) == null).count();

		if (missing > 0) {
			throw new NotFoundException("One or more features do not exist.");
		}

		internalFeatureEntries.forEach(featureDb::apply);

		return allFeatures();
	}

	@Override
	public String refresh() {
		featureDb.refresh();
		return "ok";
	}

	@Override
	public Integer count() {
		return featureDb.getFeatures().size();
	}

	@Override
	public List<String> enabledFeatures() {
		return featureDb.getFeatures()
			.values()
			.stream()
			.filter(cd.connect.features.api.FeatureState::isEnabled)
			.map(cd.connect.features.api.FeatureState::getName)
			.collect(Collectors.toList());
	}

  @Override
  public List<FeatureState> ensureExists(List<String> body) {
    featureDb.ensureExists(body);
    return allFeatures();
  }

  @Override
	public FeatureState enableFeature(String name) {
		return changeState(name, featureStateChangeService::enable);
	}

	private FeatureState changeState(String name, CheckFunc checkFunc) {
    cd.connect.features.api.FeatureState fs = featureDb.getFeature(name);

		if (fs == null) {
			throw new NotFoundException();
		}

		try {
			checkFunc.check(fs);
		} catch (BadStateException e) {
			throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
		}

		// get it again as it might have changed
    return from(featureDb.getFeature(name));
	}

	@Override
	public FeatureState disableFeature(String name) {
		return changeState(name, featureStateChangeService::disable);
	}

  @Override
  public List<String> disabledFeatures() {
    return featureDb.getFeatures()
      .values()
      .stream()
      .filter(f -> !f.isEnabled())
      .map(cd.connect.features.api.FeatureState::getName)
      .collect(Collectors.toList());
  }

  @Override
	public FeatureState lockFeature(String name) {
		return changeState(name, featureStateChangeService::lock);
	}

	@Override
	public FeatureState unlockFeature(String name) {
		return changeState(name, featureStateChangeService::unlock);
	}

	interface CheckFunc {
		void check(cd.connect.features.api.FeatureState fs) throws BadStateException;
	}
}
