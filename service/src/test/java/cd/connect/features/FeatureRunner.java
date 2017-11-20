package cd.connect.features;

import bathe.BatheBooter;
import cd.connect.war.WebAppRunner;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class FeatureRunner {
  @Test
  public void run() throws IOException {
    if (!new File("src/test/resources").exists()) {
      throw new RuntimeException("Please ensure this test is run in the home directory of the project.");
    }

    // this ensures it understands we are in "dev" move, we aren't a bundled jar file - it also ensures
    // we  get the test class loader
    new BatheBooter().runWithLoader(getClass().getClassLoader(), null, WebAppRunner.class.getName(),
      new String[]{"-Pclasspath:/feature.properties", "-P${user.home}/.webdev/feature-override.properties"});

  }
}
