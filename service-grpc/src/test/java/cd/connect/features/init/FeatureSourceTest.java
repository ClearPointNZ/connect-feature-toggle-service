package cd.connect.features.init;

import cd.connect.features.SampleFeatures;
import cd.connect.features.api.FeatureSourceStatus;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureSourceTest {
	@Test
	public void basicEnum() {
		FeatureSource fs = new FeatureSource(null, SampleFeatures.class.getName(), null);
		fs.init();
		assertThat(fs.getFeatures().size()).isEqualTo(2);
		assertThat(fs.getFeatures()).containsKey(SampleFeatures.EAT_PORKIES.name());
		assertThat(fs.getFeatures()).containsKey(SampleFeatures.RIGHT_WING_MAYHEM.name());
		assertThat(fs.getFeatures().get(SampleFeatures.RIGHT_WING_MAYHEM.name())).isEqualTo(FeatureSourceStatus.LOCKED);
		assertThat(fs.getFeatures().get(SampleFeatures.EAT_PORKIES.name())).isEqualTo(FeatureSourceStatus.ENABLED);
	}
}
