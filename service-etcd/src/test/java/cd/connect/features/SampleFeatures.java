package cd.connect.features;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public enum SampleFeatures {
  EAT_PORKIES("enabled"),
  RIGHT_WING_MAYHEM("sausage");

  private String val;

  SampleFeatures(String val) {
    this.val = val;
  }

  @Override
  public String toString() {
    return val;
  }
}
