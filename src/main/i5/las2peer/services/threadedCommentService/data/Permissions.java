package i5.las2peer.services.threadedCommentService.data;

import java.io.Serializable;

/**
 * Permissions for a CommentThread.
 * 
 * @author Jasper Nalbach
 *
 */
public class Permissions implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * The owning agent who can administer comments
	 */
	public long owner;
	/**
	 * Agent who is able to post and vote comments
	 */
	public long writer;
	/**
	 * Agent who has read-only access to thsi thread
	 */
	public long reader;
	
	
	/**
	 * Set up permission configuration for a CommentThread. Agents can have four different roles: service, owner, writer, reader
	 * 
	 * @param owner has permission to administer comments
	 * @param writer can post comments and up/downvote them
	 * @param reader read-only access
	 */
	public Permissions(long owner,long writer,long reader) {
		this.owner=owner;
		this.writer=writer;
		this.reader=reader;
	}
}
