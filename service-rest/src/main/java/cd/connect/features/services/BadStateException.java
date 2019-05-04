package cd.connect.features.services;

/**
 * Must be caught and dealt with
 *
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class BadStateException extends Exception {
	public BadStateException(String message) {
		super(message);
	}
}
