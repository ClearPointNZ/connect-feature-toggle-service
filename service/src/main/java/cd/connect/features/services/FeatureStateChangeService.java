package cd.connect.features.services;

import cd.connect.features.api.FeatureDb;
import cd.connect.features.api.FeatureState;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * I need to keep the model anemic.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureStateChangeService {
	private final FeatureDb featureDb;

	@Inject
	public FeatureStateChangeService(FeatureDb featureDb) {
		this.featureDb = featureDb;
	}

	public void enable(FeatureState fs) throws BadStateException {
		if (fs.isLocked()) {
			throw new BadStateException(String.format("state `%s` is locked, it cannot be enabled.", fs.getName()));
		}

		if (fs.getWhenEnabled() == null) {
			fs.setWhenEnabled(LocalDateTime.now());
			featureDb.apply(fs);
		}
	}

	public void disable(FeatureState fs) throws BadStateException {
		if (fs.isLocked()) {
			throw new BadStateException(String.format("state `%s` is locked, it cannot be disabled.", fs.getName()));
		}

		if (fs.getWhenEnabled() != null) {
			fs.setWhenEnabled(null);
			featureDb.apply(fs);
		}
	}

	public void lock(FeatureState fs) throws BadStateException {
		if (fs.getWhenEnabled() != null) {
			throw new BadStateException(String.format("state `%s` is enabled, it cannot be locked.", fs.getName()));
		}

		if (!fs.isLocked()) {
			fs.setLocked(true);
			featureDb.apply(fs);
		}
	}

	public void unlock(FeatureState fs) throws BadStateException {
		if (fs.getWhenEnabled() != null) {
			throw new BadStateException(String.format("state `%s` is enabled, it cannot be locked.", fs.getName()));
		}

		if (fs.isLocked()) {
			fs.setLocked(false);
			featureDb.apply(fs);
		}
	}

	public List<String> enableAll(LocalDateTime when) {
		List<String> unlockedNowEnabled = new ArrayList<>();

		if (when == null) {
			when = LocalDateTime.now();
		}

		final LocalDateTime finalWhen = when;

		featureDb.getFeatures().forEach((name, fs) -> {
			if (fs.getWhenEnabled() == null) {

				unlockedNowEnabled.add(name);

				fs.setWhenEnabled(finalWhen);

				featureDb.apply(fs);
			}
		});

		return unlockedNowEnabled;
	}

	public void validStateCheck(List<FeatureState> states) throws BadStateException {
		for (FeatureState fs : states) {
			if (fs.getWhenEnabled() != null && fs.isLocked()) {
				throw new BadStateException(String.format("Attempted to set bad state on feature `%s`, both locked and enabled.", fs.getName()));
			}
		}
	}
}
