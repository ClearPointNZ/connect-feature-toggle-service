package cd.connect.features.client;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.junit.Test;

import java.time.LocalDateTime;

import static cd.connect.features.client.FeatureContext.ACCELERATE_FEATURE_OVERRIDE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureContextTest {
	public enum SampleFeatures implements Feature {
		fred, mary, john;

		public boolean isActive() {
			return FeatureContext.isActive(this);
		}
	}

	@Test
	public void testBasicRepository() {
		FeatureContext.repository = mock(FeatureRepository.class);
		FeatureState fred = new FeatureState();
		when(FeatureContext.repository.getFeatureState(eq(SampleFeatures.fred.name()))).thenReturn(fred);
		FeatureState mary = new FeatureState();
		mary.locked = true;
		when(FeatureContext.repository.getFeatureState(eq(SampleFeatures.mary.name()))).thenReturn(mary);
		FeatureState john = new FeatureState();
		john.whenEnabled = LocalDateTime.now();
		when(FeatureContext.repository.getFeatureState(eq(SampleFeatures.john.name()))).thenReturn(john);

		assertThat(SampleFeatures.fred.isActive()).isFalse();
		assertThat(SampleFeatures.mary.isActive()).isFalse();
		assertThat(SampleFeatures.john.isActive()).isTrue();
	}

	@Test
	public void testOpenTracingOverride() {
		FeatureContext.repository = mock(FeatureRepository.class);
		
		Tracer tracer = mock(Tracer.class);
		GlobalTracer.register(tracer);

		Span span = mock(Span.class);
		when(tracer.activeSpan()).thenReturn(span);

		// override so mary is active, john is disabled
		when(span.getBaggageItem(eq(ACCELERATE_FEATURE_OVERRIDE))).thenReturn(String.format("%s=true%s=false",
			SampleFeatures.mary.name(), SampleFeatures.john.name()));

		// mary is locked, john is enabled
		FeatureState mary = new FeatureState();
		mary.locked = true;
		when(FeatureContext.repository.getFeatureState(eq(SampleFeatures.mary.name()))).thenReturn(mary);
		FeatureState john = new FeatureState();
		john.whenEnabled = LocalDateTime.now();
		when(FeatureContext.repository.getFeatureState(eq(SampleFeatures.john.name()))).thenReturn(john);


		assertThat(SampleFeatures.mary.isActive()).isTrue();
		assertThat(SampleFeatures.john.isActive()).isFalse();

	}
}
