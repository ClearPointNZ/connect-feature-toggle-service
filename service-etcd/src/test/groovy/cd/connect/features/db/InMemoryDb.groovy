package cd.connect.features.db

import com.coreos.jetcd.api.KeyValue
import com.coreos.jetcd.api.RangeResponse
import com.coreos.jetcd.kv.GetResponse
import com.coreos.jetcd.kv.PutResponse
import com.google.protobuf.ByteString
import groovy.json.JsonOutput

import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

/**
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
class InMemoryDb extends FeatureDbEtcd {
	final Map<String, String> puts = [:]
	List<String> initialFeatures

	InMemoryDb(List<String> initialFeatures) {
		this(initialFeatures, new HashSet<Consumer<FeatureDb.WatchSignal>>())
	}

	InMemoryDb(List<String> initialFeatures, Set<Consumer<FeatureDb.WatchSignal>> signalListener) {
		super(signalListener)
		this.initialFeatures = initialFeatures
	}


	@Override
	protected CompletableFuture<GetResponse> kvGet(String key) {
		if (offset == key) {
			return CompletableFuture.completedFuture(new GetResponse(
				RangeResponse.newBuilder().addKvs(KeyValue.newBuilder()
					.setKey(ByteString.copyFromUtf8("offset")).setValue(ByteString.copyFromUtf8(JsonOutput.toJson(initialFeatures)))).build()
			)
			);
		} else if (puts[key]) {
			return CompletableFuture.completedFuture(new GetResponse(
				RangeResponse.newBuilder().addKvs(KeyValue.newBuilder()
					.setKey(ByteString.copyFromUtf8(key)).setValue(ByteString.copyFromUtf8(puts[key]))).build()
			)
			);
		} else {
			throw new RuntimeException('Not expecting any other get keys but received request for ' + key)
		}
	}

	@Override
	protected CompletableFuture<PutResponse> kvPut(String key, String value) {
		puts[key] = value

		return new CompletableFuture<PutResponse>(puts)
	}

}
