package i5.las2peer.services.commentService.storage;

/**
 * A simple pointer to a Storable whithout any special capabilities. 
 * 
 * @author Jasper Nalbach
 *
 * @param <T>
 */
public class StorableWeakPointer<T extends Storable> extends StorablePointer<T> {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new WeakPointer.
	 * @param parent
	 * @param target
	 */
	protected StorableWeakPointer(Storable parent, T target) {
		super(parent, target);
	}

	/**
	 * Does nothing...
	 */
	@Override
	public void detach() throws StorageException {
		// do nothing
	}

}
