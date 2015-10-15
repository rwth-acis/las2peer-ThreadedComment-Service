package i5.las2peer.services.commentService.data;

import java.util.HashSet;
import java.util.Set;

import i5.las2peer.services.commentService.storage.PermissionException;
import i5.las2peer.services.commentService.storage.Storable;
import i5.las2peer.services.commentService.storage.StorageException;

/**
 * Simple container for votes. Uses simple lists to manage votes.
 * 
 * Lists are used instead of new Storables for each vote because this would not
 * add additional security as the Votes object has to be owned by the service
 * and removing and adding votes would be possible, too.
 * 
 * @author Jasper Nalbach
 *
 */
public class Votes extends Storable {
	
	// this should become an own service
	
	private static final long serialVersionUID = 1L;
	
	private Permissions permissions;
	
	private Set<Long> upvotes;
	private Set<Long> downvotes;
	
	public Votes(Permissions permissions) {
		this.permissions = permissions;
		this.upvotes = new HashSet<>();
		this.downvotes = new HashSet<>();
	}

	@Override
	protected void init() throws StorageException, PermissionException {
		try {
			addWriter(permissions.owner);
			addWriter(permissions.writer);
			addReader(permissions.reader);
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}

	@Override
	protected boolean cleanup() throws StorageException, PermissionException {		
		return true;
	}
	
	/**
	 * Get total rating
	 * @return total rating
	 */
	int getRating() {
		return upvotes.size() - downvotes.size();
	}
	
	/**
	 * Submit a vote
	 * @param agentId the user
	 * @param upvote true for upvote, false for downvote
	 * @throws StorageException
	 * @throws PermissionException
	 */
	void vote(long agentId, boolean upvote) throws StorageException, PermissionException {
		upvotes.remove(agentId);
		downvotes.remove(agentId);
		
		if (upvote)
			upvotes.add(agentId);
		else
			downvotes.add(agentId);
		
		save();
	}
	
	/**
	 * get vote of a user
	 * @param agentId the user
	 * @return 1 = upvote, 0 = no vote, -1 = downvote
	 */
	short getVote(long agentId) {
		if (upvotes.contains(agentId))
			return 1;
		else if(downvotes.contains(agentId))
			return -1;
		else
			return 0;
	}

}
