package ca.phon.phontalk;

/**
 * Exception used to exit ANTLR tree walkers on first error.
 *
 */
public class TreeWalkerError extends RuntimeException {

	private static final long serialVersionUID = 7608164950651740486L;

	public TreeWalkerError() {
		super();
	}

	public TreeWalkerError(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public TreeWalkerError(String message, Throwable cause) {
		super(message, cause);
	}

	public TreeWalkerError(String message) {
		super(message);
	}

	public TreeWalkerError(Throwable cause) {
		super(cause);
	}

}
