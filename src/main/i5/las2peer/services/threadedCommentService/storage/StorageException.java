package i5.las2peer.services.threadedCommentService.storage;

/**
 * To be thrown whenever an unexpected error occurs.
 * 
 * @author Jasper Nalbach
 *
 */
public class StorageException extends Exception {
	private static final long serialVersionUID = 1L;

	public StorageException() {
	}

	public StorageException(String message) {
		super(message);
	}

	public StorageException(Throwable cause) {
		super(cause);
	}

	public StorageException(String message, Throwable cause) {
		super(message, cause);
	}
}