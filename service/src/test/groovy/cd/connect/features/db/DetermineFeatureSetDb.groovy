package cd.connect.features.db

import com.coreos.jetcd.api.KeyValue
import com.coreos.jetcd.api.RangeResponse
import com.coreos.jetcd.kv.GetResponse
import com.coreos.jetcd.kv.PutResponse
import com.google.protobuf.ByteString
import groovy.json.JsonOutput

import java.util.concurrent.CompletableFuture

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
