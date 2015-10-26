package i5.las2peer.services.commentService.storage;

import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;
import i5.las2peer.security.ServiceAgent;

/**
 * Abstract class for storages.
 * 
 * 
 * @author Jasper Nalbach
 *
 */
public abstract class Storage {
	private Context context;
	private ServiceAgent service;
	
	/**
	 * Create a new Storage object
	 * @param context Usually the context of the service
	 */
	public Storage(Context context, ServiceAgent service) {
		this.context = context;
		this.service = service;
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
	 * Get the current service agent
	 * @return the service agent
	 */
	public ServiceAgent getService() {
		return this.service;
	}
	
	/**
	 * Add a Storable to this storage. Once initialized, a Storable cannot be added to another Storage.
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
	
	/**
	 * Checks if the current active agent is the given agent or is in the given group (recursive).
	 * @param agentId The given agent/group
	 * @return true, if the current agent has privileges of the given agent
	 * @throws StorageException
	 */
	public abstract boolean hasPrivileges(long agentId) throws StorageException;
}