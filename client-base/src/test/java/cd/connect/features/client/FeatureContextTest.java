package cd.connect.features.client;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDateTime;

import static cd.connect.features.client.FeatureContext.ACCELERATE_FEATURE_OVERRIDE;
import static cd.connect.features.client.FeatureContext.FEATURE_TOGGLES_ALLOW_OVERRIDE;
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

	static Tracer tracer;

	@BeforeClass
	public static void init() {
		// we can't re-register this one
		tracer = mock(Tracer.class);
		GlobalTracer.register(tracer);
	}

	@Before
	public void setup() {
		System.clearProperty(FEATURE_TOGGLES_ALLOW_OVERRIDE);
		System.clearProperty(FeatureContext.FEATURE_TOGGLES_PREFIX + SampleFeatures.mary.name());
		System.clearProperty(FeatureContext.FEATURE_TOGGLES_PREFIX + SampleFeatures.john.name());
		FeatureContext.repository = null;
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
	public void testDeveloperOverrides() {
		System.setProperty(FeatureContext.FEATURE_TOGGLES_PREFIX + SampleFeatures.mary.name(), "true");
		System.setProperty(FeatureContext.FEATURE_TOGGLES_PREFIX + SampleFeatures.john.name(), "false");
		assertThat(SampleFeatures.mary.isActive()).isTrue();
		assertThat(SampleFeatures.john.isActive()).isFalse();
	}

	@Test(expected = RuntimeException.class)
	public void noOverrideAnNoTraceFails() {

		assertThat(SampleFeatures.john.isActive()).isFalse();
	}

	@Test
	public void testOpenTracingOverride() {
		FeatureContext.repository = mock(FeatureRepository.class);

//		Tracer tracer = getTracer();
		
		Span span = mock(Span.class);
		when(tracer.activeSpan()).thenReturn(span);

		System.setProperty(FEATURE_TOGGLES_ALLOW_OVERRIDE, "true");

		// override so mary is active, john is disabled
		when(span.getBaggageItem(eq(ACCELERATE_FEATURE_OVERRIDE))).thenReturn(String.format("%s=true%s=false",
			SampleFeatures.mary.name(), SampleFeatures.john.name()));

		// mary is locked, john is enabled
		FeatureState mary = new FeatureState();
		mary.locked = true;
		FeatureState john = new FeatureState();
		john.whenEnabled = LocalDateTime.now();


		assertThat(SampleFeatures.mary.isActive()).isTrue();
		assertThat(SampleFeatures.john.isActive()).isFalse();
	}

	private Tracer getTracer() {
		Tracer tracer;

		if (GlobalTracer.isRegistered()) {
			tracer = GlobalTracer.get();
		} else {
			tracer = mock(Tracer.class);
			GlobalTracer.register(tracer);
		}

		return tracer;
	}

	@Test
	public void noPropertyMeansNoTracingOverride() {
		FeatureContext.repository = mock(FeatureRepository.class);

//		Tracer tracer = getTracer();

		Span span = mock(Span.class);
		when(tracer.activeSpan()).thenReturn(span);

		// no system property means no-override works
		when(span.getBaggageItem(eq(ACCELERATE_FEATURE_OVERRIDE))).thenReturn(String.format("%s=true",
			SampleFeatures.mary.name()));

		// mary is locked, john is enabled
		FeatureState mary = new FeatureState();
		mary.locked = true;
		when(FeatureContext.repository.getFeatureState(eq(SampleFeatures.mary.name()))).thenReturn(mary);
		assertThat(SampleFeatures.mary.isActive()).isFalse();
	}
}
