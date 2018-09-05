package cd.connect.features.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * ignore unknown properties so it is backwards compatible.
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeatureState implements Comparable<FeatureState> {
  /**
   * this allows features to be transferred by their name and mapped to their local Enum (if any)
   */
  private String name;

  /**
   * When this feature was enabled (if it was enabled).
   */
  private LocalDateTime whenEnabled;

  /**
   * Locked features cannot be enabled - this is the default state when a feature is created.
   */

  private boolean locked;

  public FeatureState(String name,  LocalDateTime whenEnabled, boolean locked) {
    this.name = name;
    this.whenEnabled = whenEnabled;
    this.locked = locked;
  }

  // for serialization
  public FeatureState() {
  }

  public FeatureState(FeatureState copy) {
    this(copy.name, copy.whenEnabled, copy.locked);
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setWhenEnabled(LocalDateTime whenEnabled) {
    this.whenEnabled = whenEnabled;
  }

  public LocalDateTime getWhenEnabled() {
    return whenEnabled;
  }

  public String getName() {
    return name;
  }

  @JsonIgnore
  public boolean isEnabled() {
    return whenEnabled != null;
  }

  public boolean isLocked() {
    return locked;
  }

  public void setLocked(boolean locked) {
    this.locked = locked;
  }

  @Override
  public int compareTo(FeatureState other) {
    return this.name.compareTo(other.name);
  }

	@Override
	public String toString() {
		return "FeatureState{" +
			"name='" + name + '\'' +
			", whenEnabled=" + whenEnabled +
			", locked=" + locked +
			'}';
	}
}
