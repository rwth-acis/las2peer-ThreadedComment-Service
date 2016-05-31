package i5.las2peer.services.threadedCommentService.data;

import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.security.AgentLockedException;
import i5.las2peer.services.threadedCommentService.storage.PermissionException;
import i5.las2peer.services.threadedCommentService.storage.Storable;
import i5.las2peer.services.threadedCommentService.storage.StorableSharedPointer;
import i5.las2peer.services.threadedCommentService.storage.StorableWeakPointer;
import i5.las2peer.services.threadedCommentService.storage.StorageException;

import java.util.Date;
import java.util.List;

/**
 * Represents a comment
 * 
 * @author Jasper Nalbach
 *
 */
public class Comment extends Storable {
	private static final long serialVersionUID = 1L;

	/**
	 * The author
	 */
	private long agentId;
	/**
	 * Date of creation
	 */
	private Date date;
	/**
	 * comment
	 */
	private String body;
	/**
	 * Indicates if the comment was edited
	 */
	private boolean edited;

	private Permissions permissions;

	private StorableWeakPointer<Comments> parent;

	/**
	 * Votes container
	 */
	private StorableSharedPointer<Votes> votes;

	/**
	 * Comments container
	 */
	private StorableSharedPointer<Comments> comments;

	public Comment(long agentId, Date date, String body) {
		super();

		this.agentId = agentId;
		this.date = date;
		this.body = body;
		this.edited = false;
	}

	@Override
	public void init() throws StorageException, PermissionException {
		// permissions
		try {
			addWriter(permissions.owner);
			addWriter(getAgentId());
			addReader(permissions.writer);
			addReader(permissions.reader);
		} catch (Exception e) {
			throw new StorageException(e);
		}

		// votes
		this.votes = sharedPointer(new Votes(permissions));

		// comments
		this.comments = sharedPointer(new Comments(permissions));
	}

	@Override
	protected boolean cleanup() throws StorageException, PermissionException {
		parent.get().removeComment(this);

		votes.detach();

		comments.detach();

		return true;
	}

	void setPermissions(Permissions perms) {
		this.permissions = perms;
	}

	void setParent(Comments comments) throws StorageException, PermissionException {
		this.parent = weakPointer(comments);
		save();
	}

	/**
	 * The author
	 * 
	 * @return agent id
	 */
	public long getAgentId() {
		return agentId;
	}

	/**
	 * date and time of creation
	 * 
	 * @return
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * comment body
	 * 
	 * @return
	 */
	public String getBody() {
		return body;
	}

	/**
	 * 
	 * @return true, if the comment was edited at least one time
	 */
	public boolean isEdited() {
		return edited;
	}

	/**
	 * Sets the body and marks the ocmment as edited
	 * 
	 * @param body New comment
	 * @throws StorageException
	 * @throws PermissionException
	 */
	public void setBody(String body) throws StorageException, PermissionException {
		try {
			if (!getStorage().getContext().hasAccess(getAgentId()))
				throw new PermissionException("Permission denied (manual check)");
		} catch (AgentNotKnownException | AgentLockedException e) {
			throw new PermissionException("Permission denied (manual check)", e);
		}

		this.body = body;
		this.edited = true;
		save();
	}

	/**
	 * Adds a vote to this comment
	 * 
	 * @param agentId The voting user.
	 * @param upvote True = upvote, false = downvote
	 * @throws StorageException
	 * @throws PermissionException
	 */
	public void vote(long agentId, boolean upvote) throws StorageException, PermissionException {
		try {
			if (!getStorage().getContext().hasAccess(permissions.writer)
					&& !getStorage().getContext().hasAccess(permissions.owner))
				throw new PermissionException("Permission denied (manual check)");
		} catch (AgentNotKnownException | AgentLockedException e) {
			throw new PermissionException("Permission denied (manual check)", e);
		}

		this.votes.get().vote(agentId, upvote);
	}

	/**
	 * Get number of upvotes
	 * 
	 * @return
	 * @throws StorageException
	 * @throws PermissionException
	 */
	public int getUpvotes() throws StorageException, PermissionException {
		return this.votes.get().getUpvotes();
	}

	/**
	 * Get number of downvotes
	 * 
	 * @return
	 * @throws PermissionException
	 * @throws StorageException
	 */
	public int getDownvotes() throws StorageException, PermissionException {
		return this.votes.get().getDownvotes();
	}

	/**
	 * Get vote of a user
	 * 
	 * @param angentId
	 * @return 1 = upvote, 0 = no vote, -1 = downvote
	 * @throws PermissionException
	 * @throws StorageException
	 */
	public short getVote(long angentId) throws StorageException, PermissionException {
		return this.votes.get().getVote(angentId);
	}

	/**
	 * Adds a new comment to this comment.
	 * 
	 * @param comment Newly created comment.
	 * @throws StorageException
	 * @throws PermissionException
	 */
	public void addComment(Comment comment) throws StorageException, PermissionException {
		try {
			if (!getStorage().getContext().hasAccess(permissions.writer)
					&& !getStorage().getContext().hasAccess(permissions.owner))
				throw new PermissionException("Permission denied (manual check)");
		} catch (AgentNotKnownException | AgentLockedException e) {
			throw new PermissionException("Permission denied (manual check)", e);
		}

		this.comments.get().addComment(comment);
	}

	/**
	 * Get a list of all comments (without sub-replys)
	 * 
	 * @return List of comments
	 * @throws StorageException
	 * @throws PermissionException
	 */
	public List<Comment> getComments() throws StorageException, PermissionException {
		return this.comments.get().getComments();
	}

	/**
	 * Get comment count
	 * 
	 * @return comment count
	 * @throws PermissionException
	 * @throws StorageException
	 */
	public int getCommentCount() throws StorageException, PermissionException {
		return this.comments.get().getCommentCount();
	}

	@Override
	public void delete() throws StorageException, PermissionException {
		try {
			if (!getStorage().getContext().hasAccess(getAgentId())
					&& !getStorage().getContext().hasAccess(permissions.owner))
				throw new PermissionException("Permission denied (manual check)");
		} catch (AgentNotKnownException | AgentLockedException e) {
			throw new PermissionException("Permission denied (manual check)", e);
		}

		super.delete();
	}
}