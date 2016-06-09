package i5.las2peer.services.threadedCommentService.storage;

import i5.las2peer.p2p.ArtifactNotFoundException;
import i5.las2peer.persistency.DecodingFailedException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.Context;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

public class DHTStorage extends Storage {

	private static final String ENVELOPE_PREFIX = "COMMENT";

	public DHTStorage(Context context) {
		super(context);
	}

	private String getEnvelopeName(String id) {
		return ENVELOPE_PREFIX + id;
	}

	@Override
	public <S extends Storable> S init(S content) throws StorageException, PermissionException {
		if (content.getStorage() != null)
			throw new StorageException("Storable is already initialized.");

		content.setStorage(this);
		content.init();
		content.save();
		content.setStored();

		return content;
	}

	@Override
	public void save(Storable content) throws StorageException, PermissionException {
		setEnvelopeData(content);
	}

	@Override
	public Storable load(Class<? extends Storable> cls, String id) throws StorageException, PermissionException,
			NotFoundException {
		Storable result = getEnvelopeData(cls, id);
		result.setStorage(this);
		return result;
	}

	@Override
	public void delete(Class<? extends Storable> cls, String id) throws StorageException, PermissionException,
			NotFoundException {
		Storable storable = getEnvelopeData(cls, id);
		setEnvelopeData(storable, true);
	}

	private Envelope createNewEnvelope(Storable storable) throws StorageException, PermissionException {
		try {
			if (storable.getWriter().size() == 0)
				throw new StorageException("Storable has no Agent with writing access!");

			// create owner group using writer list
			Agent[] ownerList = new Agent[storable.getWriter().size()];
			for (int i = 0; i < ownerList.length; i++) {
				try {
					ownerList[i] = getContext().requestAgent(storable.getWriter().get(i));
				} catch (Exception e) {
					ownerList[i] = getContext().getAgent(storable.getWriter().get(i));
				}
			}

			GroupAgent group = GroupAgent.createGroupAgent(ownerList);

			// unlock group
			for (Agent a : ownerList) {
				try {
					group.unlockPrivateKey(a);
					break;
				} catch (Exception e) {
				}
			}
			if (group.isLocked())
				throw new PermissionException("At least one writer must be unlocked in this Context!");

			// store group
			getContext().getLocalNode().storeAgent(group);

			// set owner in Storable
			storable.setOwner(group.getId());

			// store Storable
			Envelope envelope = Envelope.createClassIdEnvelope(storable, getEnvelopeName(storable.getId()), group);
			envelope.open(group);

			// add reader
			for (Long a : storable.getReader())
				envelope.addReader(getContext().getAgent(a));

			envelope.close();

			return envelope;
		} catch (L2pSecurityException | CryptoException | SerializationException | AgentException
				| EncodingFailedException | DecodingFailedException e) {
			throw new StorageException(e);
		}
	}

	private void setEnvelopeData(Storable storable) throws StorageException, PermissionException {
		setEnvelopeData(storable, false);
	}

	private void setEnvelopeData(Storable storable, boolean delete) throws StorageException, PermissionException {
		Envelope env;
		try {
			env = fetchEnvelope(storable.getClass(), storable.getId());
		} catch (StorageException | NotFoundException e) {
			if (delete)
				return;

			env = createNewEnvelope(storable);
		}

		try {
			// get owner agent
			GroupAgent ownerAgent = getContext().requestGroupAgent(storable.getOwner());

			// open envelope
			if (!env.isOpen())
				env.open(ownerAgent);

			// store
			env.setOverWriteBlindly(true);
			if (delete)
				env.updateContent("");
			else
				env.updateContent(storable);
			env.addSignature(ownerAgent);
			env.store();
			env.close();
		} catch (L2pSecurityException e) {
			throw new PermissionException(e);
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}

	private Storable getEnvelopeData(Class<? extends Storable> cls, String id) throws StorageException,
			PermissionException, NotFoundException {
		Envelope env = fetchEnvelope(cls, id);
		Storable data = null;
		if (env == null)
			throw new NotFoundException("Envelope could not be found, nor created!");

		try {
			env.open();
			data = env.getContent(cls);
			env.close();
		} catch (L2pSecurityException e) {
			throw new PermissionException(e);
		} catch (Exception e) {
			throw new StorageException(e);
		}

		return data;
	}

	private Envelope fetchEnvelope(Class<?> cls, String id) throws StorageException, NotFoundException {
		Envelope env;
		try {
			env = getContext().getStoredObject(cls, getEnvelopeName(id));
		} catch (ArtifactNotFoundException | i5.las2peer.p2p.StorageException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new StorageException(e);
		}

		return env;
	}
}
