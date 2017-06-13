package i5.las2peer.services.threadedCommentService.data;

import java.util.ArrayList;
import java.util.List;

import i5.las2peer.api.Context;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.services.threadedCommentService.storage.PermissionException;
import i5.las2peer.services.threadedCommentService.storage.Storable;
import i5.las2peer.services.threadedCommentService.storage.StorableSharedPointer;
import i5.las2peer.services.threadedCommentService.storage.StorageException;

/**
 * Comment container.
 * @author Jasper Nalbach
 *
 */
class Comments extends Storable {
	private static final long serialVersionUID = 1L;
	
	private List<StorableSharedPointer<Comment>> comments;
	
	private Permissions permissions;
		
	/**
	 * 
	 * @param permissions permission configuration; should be the same as the CommentThread
	 */
	Comments(Permissions permissions) {
		super();
		
		this.permissions = permissions;
		
		comments = new ArrayList<>();
	}
	
	@Override
	public void init() throws StorageException {
		try {
			//addWriter(permissions.service);
			addWriter(permissions.owner);
			addWriter(permissions.writer);
			addReader(permissions.reader);
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}
	
	@Override
	protected boolean cleanup() throws StorageException, PermissionException {
		for (StorableSharedPointer<Comment> c : comments) {
			comments.remove(c);
			c.detach();
		}
		return true;
	}
	
	/**
	 * Add a new comment. Use from CommentThread.
	 * @param comment
	 * @throws StorageException
	 * @throws PermissionException
	 */
	void addComment(Comment comment) throws StorageException, PermissionException {
		comment.setPermissions(this.permissions);
		
		comments.add(sharedPointer(comment));
		comment.setParent(this);
		save();
	}
	
	/**
	 * Removes a comment from the list. Intended for use from Comment class only.
	 * @param comment
	 * @throws StorageException
	 * @throws PermissionException
	 */
	void removeComment(Comment comment) throws StorageException, PermissionException {
		
		// check if admin
		boolean isAdmin = false;
		try {
			isAdmin = Context.get().hasAccess(permissions.owner);
		} catch (AgentNotFoundException | AgentOperationFailedException e) {
			throw new StorageException(e);
		}
		
		
		// custom permission check
		if (!(Context.get().getMainAgent().getIdentifier().equals(comment.getAgentId()) || isAdmin )) {
			throw new PermissionException("Comment list cannot be mondified (permission checked by the service)");
		}
		
		// remove comment
		for (StorableSharedPointer<Comment> c : comments) {
			if (c.getId().equals(comment.getId())) {
				comments.remove(c);
				break;
			}
		}
		
		save();
	}
	
	/**
	 * Fetches all comments from the stroage, excluding replys
	 * @return
	 * @throws StorageException
	 * @throws PermissionException
	 */
	List<Comment> getComments() throws StorageException, PermissionException {
		List<Comment> ret = new ArrayList<>();
		
		for (StorableSharedPointer<Comment> c : comments) {
			ret.add(c.get());
		}
		
		return ret;
	}
	
	/**
	 * Get number of comments
	 * @return number of comments
	 */
	int getCommentCount() {
		return comments.size();
	}
}