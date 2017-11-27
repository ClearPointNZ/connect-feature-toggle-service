package cd.connect.features.db

import cd.connect.features.api.FeatureState
import cd.connect.features.init.FeatureSourceStatus
import cd.connect.jackson.JacksonObjectProvider
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import spock.lang.Specification

import java.time.LocalDateTime

/**
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */

class FeatureDbEtcdSpec extends Specification {


	def "initialize features from nothing"() {
		given: 'a set of features'
		  def features = ['f1': FeatureSourceStatus.LOCKED, 'f2': FeatureSourceStatus.UNLOCKED]
		and: 'i have a new etcd db backend'
		  DetermineFeatureSetDb db = new DetermineFeatureSetDb([])
		when: 'i load these features'
		  db.ensureExists(features)
		then: 'the db contains these'
		  db.puts.size() == 3
		  db.puts[db.fnOffset('f1')] != null
		  new JsonSlurper().parseText(db.puts[db.fnOffset('f1')]).locked
		  db.puts[db.fnOffset('f2')] != null
		  !new JsonSlurper().parseText(db.puts[db.fnOffset('f2')]).locked
		  !new JsonSlurper().parseText(db.puts[db.fnOffset('f2')]).whenEnabled
		db.puts[db.offset] == '["f1","f2"]'
		and: 'we have loaded feature state of all of these feature names'
		  !db.loadedFeatures.disjoint(['f1', 'f2'])
		and: 'we have set a watch on the names'
		  db.watched == true
	}

	def "partial matches show new items"() {
		given: 'a set of features'
			def features = ['f1': FeatureSourceStatus.LOCKED, 'f2': FeatureSourceStatus.UNLOCKED]
		and: 'i have a new etcd db backend'
		  DetermineFeatureSetDb db = new DetermineFeatureSetDb(['f1', 'f3'])
		when: 'i load these features'
		  db.ensureExists(features)
		then: 'the db contains these'
			db.puts.size() == 2
		  // should only have the feature names and f2 (as f1 has not changed)
		  db.puts[db.fnOffset('f2')] != null
			!new JsonSlurper().parseText(db.puts[db.fnOffset('f2')]).locked
			!new JsonSlurper().parseText(db.puts[db.fnOffset('f2')]).whenEnabled
		  db.puts[db.offset] == '["f1","f2"]'
		and: 'we have loaded feature state of all of these feature names'
			!db.loadedFeatures.disjoint(['f1', 'f2'])
		and: 'we have set a watch on the names'
		  db.watched == true
	}

	def "load features, where one has been deleted"() {
		given: 'a set of features'
			def features = ['f1': FeatureSourceStatus.LOCKED, 'f2': FeatureSourceStatus.UNLOCKED]
		and: 'i have a new etcd db backend'
		  // this will cause f3 to be deleted and signalled
			SetupAndLoadFeaturesDb db = new SetupAndLoadFeaturesDb(['f1', 'f3'])
		  List<FeatureDb.WatchSignal> signals = []
		  db.watch({ FeatureDb.WatchSignal ws ->
			  signals.add(ws)
		  })
		and: 'i have key values for f3 and f1'
		  db.puts[db.fnOffset('f1')] = JacksonObjectProvider.mapper.writeValueAsString(new FeatureState(whenEnabled: LocalDateTime.now(), name: 'f1'))
		  db.puts[db.fnOffset('f3')] = JacksonObjectProvider.mapper.writeValueAsString(new FeatureState(locked: true, name: 'f3'))
		when: 'i load these features'
		  db.ensureExists(features)
		then:
		  signals.size() == 2
		  signals.find({it.name == 'f3' && it.deleted})
		  signals.find({it.name == 'f2' && !it.state.locked && !it.state.enabled})
		  db.puts.size() == 4
		  db.getFeatures().size() == 2

	}
}
