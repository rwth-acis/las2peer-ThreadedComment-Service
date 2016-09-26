package i5.las2peer.services.threadedCommentService.storage;

import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.tools.CryptoException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

	private void setEnvelopeData(Storable storable) throws StorageException, PermissionException {
		setEnvelopeData(storable, false);
	}

	private void setEnvelopeData(Storable storable, boolean delete) throws StorageException, PermissionException {
		Serializable content;
		if (delete) {
			content = "";
		} else {
			content = storable;
		}

		Envelope env = null;
		try { // existing
			env = getContext().fetchEnvelope(getEnvelopeName(storable.getId()));
		} catch (Exception e) {
		}

		if (env != null) {
			createNewVersion(env, storable.getOwner(), content);
		} else if (!delete) {
			createNewEnvelope(storable);
		}
	}

	private void createNewVersion(Envelope previousVersion, long owner, Serializable content)
			throws PermissionException, StorageException {
		try {
			// get owner agent
			GroupAgent ownerAgent = getContext().requestGroupAgent(owner);

			// store new version
			// TODO BUG in las2peer: wrong method is called:
			Envelope newEnv = getContext().createEnvelope(previousVersion, content);
			getContext().storeEnvelope(newEnv, ownerAgent);
		} catch (L2pSecurityException e) {
			throw new PermissionException(e);
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}

	private void createNewEnvelope(Storable storable) throws StorageException, PermissionException {
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

			GroupAgent ownerGroup = GroupAgent.createGroupAgent(ownerList);

			// unlock group
			for (Agent a : ownerList) {
				try {
					ownerGroup.unlockPrivateKey(a);
					break;
				} catch (Exception e) {
				}
			}
			if (ownerGroup.isLocked())
				throw new PermissionException("At least one writer must be unlocked in this Context!");

			// store group
			getContext().getLocalNode().storeAgent(ownerGroup);

			// set owner in Storable
			storable.setOwner(ownerGroup.getId());

			// create reader list
			List<Agent> reader = new ArrayList<>();
			reader.add(ownerGroup);
			for (Long a : storable.getReader())
				reader.add(getContext().getAgent(a));

			// store Storable
			Envelope envelope = getContext().createEnvelope(getEnvelopeName(storable.getId()), storable, reader);

			getContext().storeEnvelope(envelope, ownerGroup);
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}

	private Storable getEnvelopeData(Class<? extends Storable> cls, String id) throws StorageException,
			PermissionException, NotFoundException {
		Envelope env;

		try {
			env = getContext().fetchEnvelope(getEnvelopeName(id));
		} catch (ArtifactNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new StorageException(e);
		}

		Storable data = null;
		if (env == null)
			throw new NotFoundException("Envelope could not be found, nor created!");

		System.out.println(id);
		System.out.println(env.getReaderKeys().keySet());

		try {
			data = (Storable) env.getContent();
		} catch (CryptoException | L2pSecurityException e) {
			throw new PermissionException(e);
		} catch (Exception e) {
			throw new StorageException(e);
		}

		return data;
	}
}
