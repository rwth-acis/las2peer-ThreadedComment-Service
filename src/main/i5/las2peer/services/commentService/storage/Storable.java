package i5.las2peer.services.commentService.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An Object that can be stored in a Storage. 
 * 
 * @author Jasper Nalbach
 *
 */
public abstract class Storable implements Serializable {
	private static final long serialVersionUID = 1L;
	
	// metadata
	/**
	 * Id of the Object. Generated automatically.
	 */
	private String id;
	/**
	 * Timestamp of first instantiation.
	 */
	private long timeCreated;
	/**
	 * ID of the "owning" GroupAgent. The Envelope will be signed with this agent; all wriwters will be added to this Group.
	 */
	private long owner;
	
	// storage management
	/**
	 * Attached Storage.
	 */
	private transient Storage storage;
	/**
	 * True if the Storable has been stored in a Storage.
	 */
	private boolean stored;
	
	// right management
	/**
	 * List of writers. This variable is only used before the Storable is initialized; will be used to create the Envelope.
	 */
	private transient List<Long> writer;
	/**
	 * List of readers. This variable is only used before the Storable is initialized; will be used to create the Envelope.
	 */
	private transient List<Long> reader;
	
	// storage management
	/**
	 * Number of StorablePointer pointing to this Storable. Used by StorableSharedPointer.
	 */
	private int referenceCounter;
	
	public Storable() {
		this.reader = new ArrayList<>();
		this.writer = new ArrayList<>();
		this.id = UUID.randomUUID().toString();
		this.timeCreated = System.currentTimeMillis();
		this.owner = 0;
		
		this.referenceCounter = 0;
		this.stored = false;
	}
	
	/**
	 * Set up Pointers, Rights, ...
	 * Everything that would be done in the constructor but needs an attached Storage.
	 * 
	 * @throws StorageException
	 * @throws PermissionException
	 */
	protected abstract void init() throws StorageException, PermissionException;
	
	/**
	 * Cleans up references from and to this Storable. Developers need to keep track of "parent" elements using StorableWeakPointers to implement this method.
	 * 
	 * @return True, if the Storable is allowed to be deleted, otherwise return false.
	 * @throws StorageException
	 * @throws PermissionException
	 */
	protected abstract boolean cleanup() throws StorageException, PermissionException;
	
	/**
	 * Set the Storage.
	 * 
	 * @param s
	 */
	void setStorage(Storage s) {
		this.storage = s;
	}
	
	/**
	 * 
	 * @return The attached Storage.
	 */
	public Storage getStorage() {
		return storage;
	}
	
	/**
	 * Marks the Storable as stored.
	 */
	void setStored() {
		this.stored = true;
	}
	
	/**
	 * Increase the reference counter
	 */
	void incReferenceCounter() {
		this.referenceCounter++;
	}
	
	/**
	 * Decrease the reference counter
	 */
	void decReferenceCounter() {
		this.referenceCounter--;
	}
	
	/**
	 * 
	 * @return Number of references to this Storable
	 */
	int referenceCounter() {
		return this.referenceCounter;
	}
	
	/**
	 * 
	 * @return Id of this Storable
	 */
	public String getId() {
		return this.id;
	}
	
	/**
	 * 
	 * @return Timestamp of creation of this Storable
	 */
	public long timeCreated() {
		return timeCreated;
	}
	
	/**
	 * Create a StorableSharedPointer from this Storable to another Storable.
	 * @param <T> 
	 * @param target The targeted Storable.
	 * @return A StorableSharedPointer that should be used as non-transient attribute and to be detached when deleted.
	 * @throws StorageException
	 * @throws PermissionException
	 */
	protected <T extends Storable> StorableSharedPointer<T> sharedPointer(T target) throws StorageException, PermissionException {
		if(target.getStorage()==null)
			storage.init(target);
		else if (target.getStorage() != this.getStorage())
			throw new StorageException("Target is not in the same storage.");
		
		return new StorableSharedPointer<T>(this, target);
	}
	
	/**
	 * Create a StorableWeakPointer from thsi Storable to antoher Storable.
	 * @param <T> 
	 * @param target The targeted Storable.
	 * @return A StorableWeakPointer.
	 * @throws StorageException
	 * @throws PermissionException
	 */
	protected <T extends Storable> StorableWeakPointer<T> weakPointer(T target) throws StorageException, PermissionException {
		if(target.getStorage()==null)
			storage.init(target);
		else if (target.getStorage() != this.getStorage())
			throw new StorageException("Target is not in the same storage.");
		
		return new StorableWeakPointer<T>(this, target);
	}
	
	/**
	 * Saves this Storable. Should be called after altering attributes.
	 * @throws StorageException
	 * @throws PermissionException
	 */
	protected void save() throws StorageException, PermissionException {
		if (storage != null) {
			storage.save(this);
		}
		else
			throw new StorageException("Storable has no Storage assigned!");
	}
	
	/**
	 * Deletes the Storable. Invokes cleanup and deletes from Storable if possible.
	 * @throws StorageException
	 * @throws PermissionException
	 */
	public void delete() throws StorageException, PermissionException {
		if (!cleanup())
			throw new StorageException("Storable cannot be deleted.");
		
		// delete this object from storage
		try {
			storage.delete(this.getClass(), getId());
		}
		catch (NotFoundException e) {
			throw new StorageException("Storable does not exist", e);
		}
	}
	
	
	
	// right management
	/**
	 * Add a writer to this Storable. Can only be called before storing in a Storage.
	 * @param reader
	 * @throws StorageException
	 */
	public void addWriter(Long reader) throws StorageException {
		if (this.stored)
			throw new StorageException("Storable is already stored, permissions cannot be changed anymore.");
		
		this.writer.add(reader);
	}
	
	/**
	 * Get writer of this Storable. Can only be called before storing in a Storage.
	 * @return
	 * @throws StorageException
	 */
	protected List<Long> getWriter() throws StorageException {
		if (this.stored)
			throw new StorageException("Storable is already stored, cannot get writer this way.");
		
		return writer;
	}
	
	/**
	 * Add a reader to this Storable. Can only be called before storing in a Storage.
	 * @param reader
	 * @throws StorageException
	 */
	public void addReader(Long reader) throws StorageException {
		if (this.stored)
			throw new StorageException("Storable is already stored, permissions cannot be changed anymore.");
		
		this.reader.add(reader);
	}
	
	/**
	 * Get reader of this Storable. Can only be called before storing in a Storage.
	 * @return
	 * @throws StorageException
	 */
	protected List<Long> getReader() throws StorageException {
		if (this.stored)
			throw new StorageException("Storable is already stored, cannot get reader this way.");
		
		return reader;
	}
	
	
	/**
	 * Set the owning GroupAgent of this Storable.
	 * @param owner
	 * @throws StorageException
	 */
	void setOwner(long owner) throws StorageException {
		if (this.stored)
			throw new StorageException("This should never happen.");
		
		this.owner = owner;
	}
	
	/**
	 * Get the owning GroupAgent of this Storable
	 * @return
	 * @throws StorageException
	 */
	long getOwner() throws StorageException {
		return owner;
	}
}
