package i5.las2peer.services.commentService.data;

import java.util.ArrayList;
import java.util.List;

import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.services.commentService.storage.PermissionException;
import i5.las2peer.services.commentService.storage.Storable;
import i5.las2peer.services.commentService.storage.StorableSharedPointer;
import i5.las2peer.services.commentService.storage.StorageException;

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
		Agent a;
		try {
			a = getStorage().getContext().getAgent(permissions.owner);
		} catch (AgentNotKnownException e) {
			throw new StorageException(e);
		}
		if (a instanceof GroupAgent) {
			isAdmin = ((GroupAgent)a).isMemberRecursive(getStorage().getContext().getMainAgent());
		}
		else {
			isAdmin = getStorage().getContext().getMainAgent().getId() == a.getId();
		}
		
		
		// custom permission check
		if (!(getStorage().getContext().getMainAgent().getId() == comment.getAgentId() || isAdmin )) {
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