package i5.las2peer.services.commentService.storage;

import i5.las2peer.security.Context;

/**
 * Abstract class for storages.
 * 
 * 
 * @author Jasper Nalbach
 *
 */
public abstract class Storage {
	private Context context;
	
	/**
	 * Create a new Storage object
	 * @param context Usually the context of the service
	 */
	public Storage(Context context) {
		this.context = context;
	}
	
	/**
	 * Get the context to work with.
	 * 
	 * @return the context
	 */
	public Context getContext() {
		return context;
	}
	
	/**
	 * Add a Storable to this storage. Once initialized, a Storable cannot be added to another Storage.
	 * @param <S> 
	 * 
	 * @param content The Object to be initilized.
	 * @return The initilized Object.
	 * @throws StorageException
	 * @throws PermissionException
	 */
	public abstract <S extends Storable> S init(S content) throws StorageException, PermissionException;
	
	/**
	 * Save a Storable. Save a new version of the Storable.
	 * 
	 * @param content The Object to be stored.
	 * @throws StorageException
	 * @throws PermissionException
	 */
	public abstract void save(Storable content) throws StorageException, PermissionException;
	
	/**
	 * Get an Object from the storage.
	 * 
	 * @param cls Class of the stored Object
	 * @param id Id of the stored Object
	 * @return The stored Object.
	 * @throws StorageException
	 * @throws PermissionException
	 * @throws NotFoundException 
	 */
	public abstract Storable load(Class<? extends Storable> cls, String id) throws StorageException, PermissionException, NotFoundException;
	
	/**
	 * Delete an Object form the Storage.
	 * 
	 * @param cls Class of the Object.
	 * @param id Id of the Object.
	 * @throws StorageException
	 * @throws PermissionException
	 * @throws NotFoundException 
	 */
	public abstract void delete(Class<? extends Storable> cls, String id) throws StorageException, PermissionException, NotFoundException;
}