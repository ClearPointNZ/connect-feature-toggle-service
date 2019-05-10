package cd.connect.features.init;

import cd.connect.features.SampleFeatures;
import cd.connect.features.api.FeatureSourceStatus;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureSourceTest {
	@Test
	public void basicEnum() {
	  v(SampleFeatures.class);

		FeatureSource fs = new FeatureSource(null, String.format("%s,%s:ENABLED",
      SampleFeatures.EAT_PORKIES.name(), SampleFeatures.RIGHT_WING_MAYHEM.name()));

		assertThat(fs.getFeatures().size()).isEqualTo(2);
		assertThat(fs.getFeatures()).containsKey(SampleFeatures.EAT_PORKIES.name());
		assertThat(fs.getFeatures()).containsKey(SampleFeatures.RIGHT_WING_MAYHEM.name());
		assertThat(fs.getFeatures().get(SampleFeatures.RIGHT_WING_MAYHEM.name())).isEqualTo(FeatureSourceStatus.ENABLED);
		assertThat(fs.getFeatures().get(SampleFeatures.EAT_PORKIES.name())).isEqualTo(FeatureSourceStatus.LOCKED);
	}

	private void v(Class<? extends Enum> type) {


  }
}
