package i5.las2peer.services.commentService.storage;

/**
 * A shared pointer to a Storable. When the last pointer is detached, the targeted Storable will be deleted automatically.
 * @author Jasper Nalbach
 *
 * @param <T>
 */
public class StorableSharedPointer<T extends Storable> extends StorablePointer<T> {
	private static final long serialVersionUID = 1L;
	boolean valid;

	/**
	 * Creates a pointer and increases the reference counter.
	 * @param parent Parent
	 * @param target Target
	 */
	protected StorableSharedPointer(Storable parent, T target) {
		super(parent, target);
		
		this.valid = true;
		
		target.incReferenceCounter();
	}

	/**
	 * Decreases the reference coutner of the target and deletes it if neccessary
	 */
	@Override
	public void detach() throws StorageException, PermissionException {
		if (!valid) return;
		this.valid=false;
				
		get().decReferenceCounter();
			
		if (get().referenceCounter()==0)
			get().delete();
		
	}

}
