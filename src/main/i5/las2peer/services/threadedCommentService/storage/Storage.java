package i5.las2peer.services.threadedCommentService.storage;

import i5.las2peer.api.Context;
import i5.las2peer.api.persistency.Envelope;
import i5.las2peer.api.persistency.EnvelopeAccessDeniedException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.persistency.EnvelopeOperationFailedException;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.GroupAgent;

import java.io.Serializable;

public class Storage {

	public <S extends Storable> S init(S content) throws StorageException, PermissionException {
		if (content.getStorage() != null)
			throw new StorageException("Storable is already initialized.");

		content.setStorage(this);
		content.init();
		content.save();
		content.setStored();

		return content;
	}

	public void save(Storable content) throws StorageException, PermissionException {
		setEnvelopeData(content);
	}

	public Storable load(String id) throws StorageException, PermissionException,
			NotFoundException {
		Storable result = getEnvelopeData(id);
		result.setStorage(this);
		return result;
	}

	public void delete(String id) throws StorageException, PermissionException,
			NotFoundException {
		Storable storable = getEnvelopeData(id);
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
			env = Context.get().requestEnvelope(storable.getId());
			env.setContent(content);
			Context.get().storeEnvelope(env);
		} catch (EnvelopeNotFoundException e) {
			if (!delete) { // create new
				createNewEnvelope(storable);
			}
		} catch (EnvelopeAccessDeniedException e) {
			throw new PermissionException(e);
		} catch (EnvelopeOperationFailedException e) {
			throw new StorageException(e);
		}
	}

	private void createNewEnvelope(Storable storable) throws StorageException, PermissionException {
		try {
			if (storable.getWriter().isEmpty())
				throw new StorageException("Storable has no Agent with writing access!");

			// create owner group using writer list
			Agent[] ownerList = new Agent[storable.getWriter().size()];
			for (int i = 0; i < ownerList.length; i++) {
				try {
					ownerList[i] = Context.get().requestAgent(storable.getWriter().get(i));
				} catch (Exception e) {
					ownerList[i] = Context.get().fetchAgent(storable.getWriter().get(i));
				}
			}

			// create group + store it
			GroupAgent ownerGroup = Context.get().createGroupAgent(ownerList);
			Context.get().storeAgent(ownerGroup);
			
			// create envelope
			Envelope envelope = Context.get().createEnvelope(storable.getId(), ownerGroup);
			envelope.setContent(storable);
			
			// add reader
			for (String a : storable.getReader()) {
				envelope.addReader(Context.get().fetchAgent(a));
			}
			
			// store envelope
			Context.get().storeEnvelope(envelope, ownerGroup);
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}

	private Storable getEnvelopeData(String id) throws StorageException,
			PermissionException, NotFoundException {
		try {
			Envelope env = Context.get().requestEnvelope(id);
			return (Storable) env.getContent();
		} catch (EnvelopeNotFoundException e) {
			throw new NotFoundException(e);
		} catch (EnvelopeAccessDeniedException e) {
			throw new PermissionException(e);
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}
}
