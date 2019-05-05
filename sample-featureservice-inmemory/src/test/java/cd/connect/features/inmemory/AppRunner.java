package cd.connect.features.inmemory;

import bathe.BatheBooter;
import org.junit.Test;

import java.io.IOException;

public class AppRunner {
  @Test
  public void run() throws Exception {
    new BatheBooter().run(new String[]{"-R" + Application.class.getName(), "-Pclasspath:/application.properties"});
  }
}
