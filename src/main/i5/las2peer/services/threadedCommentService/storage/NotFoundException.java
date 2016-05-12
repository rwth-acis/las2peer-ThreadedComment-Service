package i5.las2peer.services.threadedCommentService.storage;

/**
 * To be thrown whenever a Storable is not found.
 * 
 * @author Jasper Nalbach
 *
 */
public class NotFoundException extends Exception {
	private static final long serialVersionUID = 1L;

	public NotFoundException() {
    }

    public NotFoundException(String message) {
        super (message);
    }

    public NotFoundException(Throwable cause) {
        super (cause);
    }

    public NotFoundException(String message, Throwable cause) {
        super (message, cause);
    }
}
