package cd.connect.features.resource.jersey;

import cd.connect.features.FeatureService;
import cd.connect.features.api.FeatureDb;
import cd.connect.features.services.FeatureStateChangeService;
import org.glassfish.jersey.internal.inject.AbstractBinder;

import javax.inject.Singleton;

public class FeatureBinder extends AbstractBinder {
  private final FeatureDb db;

  public FeatureBinder(FeatureDb db) {
    this.db = db;
  }

  @Override
  protected void configure() {
    bind(db).in(Singleton.class).to(FeatureDb.class);
    bind(new FeatureStateChangeService(db)).in(Singleton.class).to(FeatureStateChangeService.class);
//    bind(FeatureResource.class).in(Singleton.class).to(FeatureService.class);
  }
}
