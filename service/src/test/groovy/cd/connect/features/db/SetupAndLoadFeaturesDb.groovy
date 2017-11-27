package cd.connect.features.db

/**
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
class SetupAndLoadFeaturesDb extends InMemoryDb {
	SetupAndLoadFeaturesDb(List<String> initialFeatures) {
		super(initialFeatures)
	}

	@Override
	protected void watchForFeatureNameChanges() {
	}

	@Override
	protected void watchForFeatureChanges(Set<String> featureNames) {
	}
}
