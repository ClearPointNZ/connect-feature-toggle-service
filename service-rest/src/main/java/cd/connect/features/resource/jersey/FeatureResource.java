package cd.connect.features.resource.jersey;

import cd.connect.features.api.FeatureService;
import cd.connect.features.api.FeatureState;
import cd.connect.features.db.FeatureDb;
import cd.connect.features.services.BadStateException;
import cd.connect.features.services.FeatureStateChangeService;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
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

	@Override
	public FeatureState getFeature(String feature_name) {
		FeatureState fs = featureDb.getFeature(feature_name);

		if (fs == null) {
			throw new NotFoundException("No such feature");
		}

		return fs;
	}

	@Override
	public List<FeatureState> all_features() {
		return new ArrayList<>(featureDb.getFeatures().values());
	}

	@Override
	public List<FeatureState> applyAll(List<FeatureState> entries) {
		Map<String, FeatureState> states = featureDb.getFeatures();

		try {
			featureStateChangeService.validStateCheck(entries);
		} catch (BadStateException e) {
			throw new BadRequestException(e.getMessage());
		}

		long missing = entries.stream().filter(s -> s.getName() == null || states.get(s.getName()) == null).count();

		if (missing > 0) {
			throw new NotFoundException("One or more features do not exist.");
		}

		entries.forEach(featureDb::apply);

		return all_features();
	}

	@Override
	public void refresh() {
		featureDb.refresh();
	}

	@Override
	public int count() {
		return featureDb.getFeatures().size();
	}

	@Override
	public List<String> enabled() {
		return featureDb.getFeatures()
			.values()
			.stream()
			.filter(FeatureState::isEnabled)
			.map(FeatureState::getName)
			.collect(Collectors.toList());
	}

	@Override
	public List<String> enableAll(LocalDateTime when) {
		return featureStateChangeService.enableAll(when);
	}

	@Override
	public String enable(String name) {
		return changeState(name, featureStateChangeService::enable);
	}

	private String changeState(String name, CheckFunc checkFunc) {
		FeatureState fs = featureDb.getFeature(name);

		if (fs == null) {
			throw new NotFoundException();
		}

		try {
			checkFunc.check(fs);
		} catch (BadStateException e) {
			throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
		}

		return fs.getName();
	}

	@Override
	public String disable(String name) {
		return changeState(name, featureStateChangeService::disable);
	}

	@Override
	public String lock(String name) {
		return changeState(name, featureStateChangeService::lock);
	}

	@Override
	public String unlock(String name) {
		return changeState(name, featureStateChangeService::unlock);
	}

	interface CheckFunc {
		void check(FeatureState fs) throws BadStateException;
	}
}
