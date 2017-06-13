package i5.las2peer.services.threadedCommentService.data;

import i5.las2peer.api.Context;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.services.threadedCommentService.storage.PermissionException;
import i5.las2peer.services.threadedCommentService.storage.Storable;
import i5.las2peer.services.threadedCommentService.storage.StorableSharedPointer;
import i5.las2peer.services.threadedCommentService.storage.StorageException;

import java.util.List;

/**
 * 
 * User can post Comments to a CommentThread. A CommentThread has permissions set up on creation.
 * 
 * @author Jasper Nalbach
 *
 */
public class CommentThread extends Storable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Pointer to Comments container. Comment list is stored seperately to prevent illegal access.
	 */
	private StorableSharedPointer<Comments> comments;
	
	/**
	 * Permission configuration object
	 */
	private Permissions permissions;
	
	/**
	 * Creates a comment thread
	 * @param permissions permission configuration
	 */
	public CommentThread(Permissions permissions) {
		super();
				
		this.permissions = permissions;
	}
	
	@Override
	public void init() throws StorageException, PermissionException {		
		addWriter(permissions.owner);
		//addWriter(permissions.service);
		addReader(permissions.writer);
		addReader(permissions.reader);
		
		this.comments = sharedPointer(new Comments(permissions));
	}
	
	@Override
	protected boolean cleanup() throws StorageException, PermissionException {
		this.comments.detach();
		return true;
	}
	
	/**
	 * Adds a new comment to this thread.
	 * @param comment Newly created comment.
	 * @throws StorageException
	 * @throws PermissionException
	 */
	public void addComment(Comment comment) throws StorageException, PermissionException {
		// check permissions to avoid storage "zombies" (otherwise a comment object may be stored but never attached to a thread)
		try {
			if (!Context.get().hasAccess(permissions.writer) && !Context.get().hasAccess(permissions.owner))
				throw new PermissionException("Permission denied (manual check)");
		} catch (AgentNotFoundException | AgentOperationFailedException e) {
			throw new PermissionException("Permission denied (manual check)", e);
		}
			
		this.comments.get().addComment(comment);
	}
	
	/**
	 * Get a list of all comments (without replys)
	 * @return List of comments
	 * @throws StorageException
	 * @throws PermissionException
	 */
	public List<Comment> getComments() throws StorageException, PermissionException {
		return this.comments.get().getComments();
	}
	
	/**
	 * Get the permissions object
	 * @return the Permisions object
	 */
	public Permissions getPermissions() {
		return this.permissions;
	}
	
	@Override
	public void delete() throws StorageException, PermissionException {
		try {
			if (!Context.get().hasAccess(permissions.owner))
				throw new PermissionException("Permission denied (manual check)");
		} catch (AgentNotFoundException | AgentOperationFailedException e) {
			throw new PermissionException("Permission denied (manual check)", e);
		}
		
		super.delete();
	}
}