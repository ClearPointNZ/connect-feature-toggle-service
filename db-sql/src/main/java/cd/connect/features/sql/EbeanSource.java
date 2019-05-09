package cd.connect.features.sql;

import io.ebean.EbeanServer;

import javax.sql.DataSource;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public interface EbeanSource {
  EbeanServer getEbeanServer();
  DataSource getDatasource();
}
