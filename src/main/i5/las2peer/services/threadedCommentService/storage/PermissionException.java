package i5.las2peer.services.threadedCommentService.storage;

/**
 * To be thrown whenever the current user does not have permission do get/edit/delete a Storable.
 * 
 * @author Jasper Nalbach
 *
 */
public class PermissionException extends Exception {
	private static final long serialVersionUID = 1L;

	public PermissionException() {
    }

    public PermissionException(String message) {
        super (message);
    }

    public PermissionException(Throwable cause) {
        super (cause);
    }

    public PermissionException(String message, Throwable cause) {
        super (message, cause);
    }
}