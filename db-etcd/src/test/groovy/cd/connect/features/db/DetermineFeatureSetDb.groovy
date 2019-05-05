package cd.connect.features.db

/**
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
class DetermineFeatureSetDb extends InMemoryDb {
	def watched = false
	def loadedFeatures = []

	DetermineFeatureSetDb(List<String> initialFeatures) {
		super(initialFeatures)
	}

	@Override
	protected void watchForFeatureNameChanges() {
		watched = true
	}

	@Override
	protected void loadFeatures(Set<String> featureNames) {
		loadedFeatures = featureNames
	}
}
