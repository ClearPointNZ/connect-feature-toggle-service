package cd.connect.features.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.List;

@Consumes(value = MediaType.APPLICATION_JSON)
@Path("/features")
public interface FeatureService {
  @GET
  @Produces(value = MediaType.APPLICATION_JSON)
  List<FeatureState> all_features();

  /**
   * Apply enabled state from all the supplied entries and then
   * return all current entries.
   */
  @PUT
  @Produces(value = MediaType.APPLICATION_JSON)
  List<FeatureState> applyAll(List<FeatureState> entries);

  /**
   * reload state from db and republish to all clients
   */
  @PUT
  @Path("refresh")
  @Produces(value = MediaType.APPLICATION_JSON)
  void refresh();

  /**
   * total number of features
   */
  @GET
  @Path("count")
  int count();

  /**
   * Return the count of entries.
   */
  @GET
  @Path("enabled-count")
  @Produces(value = MediaType.APPLICATION_JSON)
  List<String> enabled();

  /**
   * Enable all the unlocked features. Useful in testing. Returns previously disabled ones.
   */
  @PUT
  @Path("enable-all")
  @Produces(value = MediaType.APPLICATION_JSON)
  List<String> enableAll(LocalDateTime when);
  
  /**
   * Enable the feature.
   */
  @PUT
  @Path("enable/{name}")
  String enable(@PathParam("name") String name);

  /**
   * Disable the feature.
   */
  @PUT
  @Path("disable/{name}")
  String disable(@PathParam("name") String name);

  /**
   * Lock the feature.
   */
  @PUT
  @Path("lock/{name}")
  String lock(@PathParam("name") String name);

  /**
   * Unlock the feature.
   */
  @PUT
  @Path("unlock/{name}")
  String unlock(@PathParam("name") String name);
}
