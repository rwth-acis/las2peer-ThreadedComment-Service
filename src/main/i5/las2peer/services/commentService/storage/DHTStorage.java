package i5.las2peer.services.commentService.storage;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.ArtifactNotFoundException;
import i5.las2peer.persistency.DecodingFailedException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.Context;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

public class DHTStorage extends Storage {
	
	private static final String ENVELOPE_PREFIX = "COMMENT";
	
	private Hashtable<Long, GroupAgent> groupAgents = new Hashtable <Long, GroupAgent> ();
	
	public DHTStorage(Context context, ServiceAgent service) {
		super(context,service);
	}
	
	private String getEnvelopeName(String id) {
		return ENVELOPE_PREFIX + id;
	}
	
	@Override
	public <S extends Storable> S init(S content) throws StorageException, PermissionException {
		if (content.getStorage()!=null)
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
	public Storable load(Class<? extends Storable> cls, String id) throws StorageException, PermissionException, NotFoundException {
		Storable result = getEnvelopeData(cls, id);
		result.setStorage(this);
		return result;
	}
	
	@Override
	public void delete(Class<? extends Storable> cls, String id) throws StorageException, PermissionException, NotFoundException {
		Storable storable = getEnvelopeData(cls, id);
		setEnvelopeData(storable,true);
	}
	

	private Envelope createNewEnvelope(Storable storable) throws StorageException, PermissionException {
		try {
			if (storable.getWriter().size()==0)
				throw new StorageException("Storable has no Agent with writing access!");
			
			// create owner group using writer list
			Agent[] ownerList = new Agent[storable.getWriter().size()];
			for (int i=0;i<ownerList.length;i++) {
				try {
					ownerList[i] = requestAgent(storable.getWriter().get(i));
				}
				catch(Exception e) {
					ownerList[i] = getContext().getAgent(storable.getWriter().get(i));
				}
			}
			
			GroupAgent group = GroupAgent.createGroupAgent(ownerList);
			
			// unlock group
			for (Agent a : ownerList) {
				try {
					group.unlockPrivateKey(a);
					break;
				}
				catch (Exception e) {
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
		}
		catch (L2pSecurityException | CryptoException | SerializationException | AgentException | UnsupportedEncodingException | EncodingFailedException | DecodingFailedException e) {
			throw new StorageException(e);
		}
	}
	
	private void setEnvelopeData(Storable storable) throws StorageException, PermissionException {
		setEnvelopeData(storable,false);
	}
	
	private void setEnvelopeData(Storable storable, boolean delete) throws StorageException, PermissionException {
		Envelope env;
		try {
			env = fetchEnvelope(storable.getClass(), storable.getId());
		}
		catch (StorageException | NotFoundException e) {
			if (delete)
				return;
			
			env = createNewEnvelope(storable);
		}
		
		try {		
			// get owner agent
			GroupAgent ownerAgent = (GroupAgent)requestAgent(storable.getOwner());
			
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
		}
		catch (L2pSecurityException e) {
			throw new PermissionException(e);
		}
		catch (Exception e) {
			throw new StorageException(e);
		}
	}
	
	private Storable getEnvelopeData(Class<? extends Storable> cls, String id) throws StorageException, PermissionException, NotFoundException {
		Envelope env = fetchEnvelope(cls, id);
		Storable data = null;
		if (env == null)
			throw new NotFoundException("Envelope could not be found, nor created!");
		
		try {
			tryOpen(env);
			data = env.getContent(cls);
			env.close();
		}
		catch (L2pSecurityException e) {
			throw new PermissionException(e);
		}
		catch (Exception e) {
			throw new StorageException(e);
		}

		return data;
	}

	private Envelope fetchEnvelope(Class<?> cls, String id) throws StorageException, NotFoundException {
		Envelope env;
		try {
			env = getContext().getStoredObject(cls, getEnvelopeName(id));
		}
		catch (ArtifactNotFoundException | i5.las2peer.p2p.StorageException e) {
			throw new NotFoundException(e);
		}
		catch (Exception e) {
			throw new StorageException(e);
		}

		return env;
	}

	@Override
	public boolean hasPrivileges(long agentId) throws StorageException {
			Agent agent;
			try {
				agent = getContext().getAgent(agentId);
			}
			catch (AgentNotKnownException e) {
				throw new StorageException(e);
			}
			Agent current = getContext().getMainAgent();
			
			if (agent instanceof GroupAgent) {
				return ((GroupAgent)agent).isMemberRecursive(current);
			}
			else {
				return agent.getId() == current.getId();
			}
	}

	// needed until recursive group unlocking is implemented in core
	// tries to unlock the given agent
	public Agent requestAgent(long agentId) throws StorageException, PermissionException {
		Agent current = getContext().getMainAgent();
				
		// check if it's the current agent
		if (current.getId() == agentId) 
			return current;
		
		// check if it's a known GroupAgent
		GroupAgent g = groupAgents.get(agentId);
		if (g!=null) return g;
		
		// get the agent from storage
		Agent agent;
		try {
			agent = getContext().getAgent(agentId);
		}
		catch (AgentNotKnownException e) {
			throw new StorageException(e);
		}
		
		if (agent instanceof GroupAgent) {
			// try to unlock the group
			GroupAgent group = (GroupAgent) agent;
			
			if (group.isMemberRecursive(current)) {
				// unlock the group
				try {
					unlockGroupAgentRecursive(group,current);
				}
				catch (AgentNotKnownException | L2pSecurityException | SerializationException | CryptoException e) {
				}
				
				groupAgents.put(agentId, group);
				return group;
			}
			else {
				throw new PermissionException("requesting Agent failed");
			}
		}
		else {
			throw new PermissionException("requesting Agent failed");
		}
	}
	
	private void unlockGroupAgentRecursive(GroupAgent group, Agent current) throws L2pSecurityException, SerializationException, CryptoException, AgentNotKnownException {
		if (group.isMember(current)) {
			if (group.isLocked())
				group.unlockPrivateKey(current);
			return;
		}
		
		for (Long memberId : group.getMemberList()) {
			Agent tmpAgent = getContext().getAgent(memberId);
			
			if (tmpAgent instanceof GroupAgent) {
				GroupAgent tmpGroup = (GroupAgent) tmpAgent;
				
				if (tmpGroup.isMemberRecursive(current)) {
					if (tmpGroup.isLocked())
						unlockGroupAgentRecursive(tmpGroup,current);
					
					group.unlockPrivateKey(tmpGroup);
					
					return;
				}
			}
		}
	}
	
	// method to unlock an Evnelope using the current context until Envelope->openEnvelope() is fixed
	private void tryOpen(Envelope envelope) throws StorageException, DecodingFailedException, L2pSecurityException, PermissionException {
		try {
			envelope.open ( getContext().getMainAgent() );
		}
		catch ( L2pSecurityException e ) {
			for ( long groupId : envelope.getReaderGroups() ) {
				try {
					GroupAgent group = (GroupAgent)requestAgent(groupId);
					envelope.open ( group );
					return;
				}
				catch (PermissionException e1) {
				}
				catch (Exception e1) {
					throw e1;
				}
			}
			throw e;
		}
	}
}
