package i5.las2peer.services.threadedCommentService.storage;

import java.io.Serializable;

/**
 * Base class.
 * @author Jasper Nalbach
 *
 * @param <T> Type of the target.
 */

public abstract class StorablePointer<T extends Storable> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Id of the target
	 */
	private String id;
	
	/**
	 * Reference to the parent
	 */
	private Storable parent;
	
	/**
	 * Reference to the target
	 */
	private transient T cache;
	
	/**
	 * Construct a new StorablePointer
	 * @param parent Parent element
	 * @param target Target element
	 */
	protected StorablePointer(Storable parent, T target) {
		this.id = target.getId();
		this.cache = target;
		this.parent = parent;
	}
	
	/**
	 * Get the target Storable
	 * @return The Storable
	 * @throws StorageException
	 * @throws PermissionException
	 */
	@SuppressWarnings("unchecked")
	public T get() throws StorageException, PermissionException {
		if (this.cache == null)
			try {
				this.cache = (T)parent.getStorage().load(this.id);
			} catch (NotFoundException e) {
				throw new StorageException("Target not found! Storage is inconsistent", e);
			}
			
		return this.cache;
	}
	
	/**
	 * To be called before deleting the Pointer. Checks reference counter etc
	 * @throws StorageException
	 * @throws PermissionException
	 */
	public abstract void detach() throws StorageException, PermissionException;
	
	/**
	 * 
	 * @return Id of the target Storable
	 */
	public String getId() {
		return id;
	}
}
