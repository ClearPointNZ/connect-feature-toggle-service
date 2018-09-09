package cd.connect.features.sql;

import cd.connect.features.api.FeatureState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@Entity(name = "feature_state")
public class SqlFeatureState {
	@Id
	private String name;
	@Column(nullable = true)
	private LocalDateTime whenEnabled;
	@Column(nullable = false, name = "feature_locked")
	private boolean locked;

	public SqlFeatureState() {
	}

	public SqlFeatureState(String name, LocalDateTime whenEnabled, boolean locked) {
		this.name = name;
		this.whenEnabled = whenEnabled;
		this.locked = locked;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDateTime getWhenEnabled() {
		return whenEnabled;
	}

	public void setWhenEnabled(LocalDateTime whenEnabled) {
		this.whenEnabled = whenEnabled;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public static SqlFeatureState fromFeatureState(FeatureState fs) {
		return new SqlFeatureState(fs.getName(), fs.getWhenEnabled(), fs.isLocked());
	}

	public FeatureState toFeatureState() {
		return SqlFeatureState.fromSqlFeatureState(this);
	}

	public static FeatureState fromSqlFeatureState(SqlFeatureState fs) {
		return new FeatureState(fs.getName(), fs.getWhenEnabled(), fs.isLocked());
	}


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SqlFeatureState that = (SqlFeatureState) o;
    return isLocked() == that.isLocked() &&
      Objects.equals(getName(), that.getName()) &&
      Objects.equals(getWhenEnabled(), that.getWhenEnabled());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getWhenEnabled(), isLocked());
  }
}
