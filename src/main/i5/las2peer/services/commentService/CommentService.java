package i5.las2peer.services.commentService;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import i5.las2peer.api.Service;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.restMapper.annotations.Version;
import i5.las2peer.restMapper.tools.ValidationResult;
import i5.las2peer.restMapper.tools.XMLCheck;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentLockedException;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.commentService.data.Comment;
import i5.las2peer.services.commentService.data.CommentThread;
import i5.las2peer.services.commentService.data.Permissions;
import i5.las2peer.services.commentService.storage.DHTStorage;
import i5.las2peer.services.commentService.storage.NotFoundException;
import i5.las2peer.services.commentService.storage.PermissionException;
import i5.las2peer.services.commentService.storage.Storage;
import i5.las2peer.services.commentService.storage.StorageException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;


/**
 * LAS2peer Comment Service
 * 
 * A comment service. Other services can create a new thread and set up permissions. Users
 * can post comments to this thread, reply to other comments and up/dwonvote comments.
 * 
 * @author Jasper Nalbach
 *
 */
@Path("/comments")
@Version("0.1")
@Api
@SwaggerDefinition(
		info = @Info(
				title = "LAS2peer Comment Service",
				version = "0.1",
				description = "A LAS2peer Comment Service intended to be integrated with other services.",
				termsOfService = "http://your-terms-of-service-url.com",
				contact = @Contact(
						name = "Jasper Nalbach",
						url = "provider.com",
						email = "nalbach@dbis.rwth-aachen.de"
				),
				license = @License(
						name = "your software license name",
						url = "http://your-software-license-url.com"
				)
		))
public class CommentService extends Service {
	
	public CommentService() {
		setFieldValues();
	}

	/**
	 * Create a new storage for the current context
	 * @return A Storage with the current context
	 * @throws StorageException 
	 */
	private Storage getStorage() throws StorageException {
		return new DHTStorage(this.getContext());
	}
	
	
	// helper
	
	private CommentThread _getCommentThread(String id) throws StorageException, PermissionException, NotFoundException {
		return (CommentThread) getStorage().load(CommentThread.class, id);
	}
	
	private Comment _getComment(String id) throws StorageException, PermissionException, NotFoundException {
		return (Comment) getStorage().load(Comment.class, id);
	}
	
	private JSONObject _serializeComment(Comment comment, Agent author) throws StorageException, PermissionException {
		JSONObject json = new JSONObject();
		
		json.put("id", comment.getId());
		json.put("author", _serializeAuthor(author));
		json.put("date", (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(comment.getDate()) );
		json.put("body",comment.getBody());
		json.put("rating", comment.getRating());
		json.put("myRating", comment.getVote(getContext().getMainAgent().getId()));
		json.put("replyCount", comment.getCommentCount());
		
		return json;
	}
	
	private JSONObject _serializeAuthor(Agent agent) {
		JSONObject json = new JSONObject();
		
		if (agent instanceof UserAgent) 
			json.put("name",((UserAgent)agent).getLoginName());
		else 
			json.put("name","Unknown");
		
		json.put("id", agent.getId());
		json.put("isMe", agent.getId() == getContext().getMainAgent().getId() );

		return json;
	}
	

	// //////////////////////////////////////////////////////////////////////////////////////
	// Service methods.
	// //////////////////////////////////////////////////////////////////////////////////////
	
	// RMI
	
	/**
	 * Create a new comment thread.
	 * At the moment, the MainAgent of the Context will be the ServiceAgent of the calling Service.
	 * Because of that the ServiceAgent must be also an owner!
	 * 
	 * @param owner Id of the owning agent. This agent is able to administer comments.
	 * @param writer Id of the agent who is able to post new comments and up/downvote them.
	 * @param reader Id of the agent with read-only access.
	 * @return Id of the comment thread to be stored by the using serivce.
	 */
	public String createCommentThread(long owner, long writer, long reader) {
		try {
			CommentThread thread = getStorage().init(new CommentThread(new Permissions(getAgent().getId(),owner,writer,reader)));
			return thread.getId();
		} catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Delete a comment thread.
	 * 
	 * @param id Id of the thread.
	 * @return true, if successful
	 */
	public boolean deleteCommentThread(String id) {
		try {
			_getCommentThread(id).delete();
			
			return true;
		} catch (Exception e) {
			//e.printStackTrace();
			return false;
		}
		
	}
	
	
	// REST
	
	/**
	 * Get the comment thread including comments.
	 * @param threadId Id of the thread
	 * @return 
	 */
	@GET
	@Path("/threads/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Comment Thread"),
			@ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "Forbidden"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Not Found"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal Server Error")
	})
	@ApiOperation(value = "getCommentThread",
			notes = "Get comment thread including comments")
	public HttpResponse getCommentThread(@PathParam("id") String threadId) {
		try {
			CommentThread thread = _getCommentThread(threadId);
			List<Comment> comments = thread.getComments();
			
			JSONArray list = new JSONArray();
			for(Comment comment : comments) {
				list.add(_serializeComment(comment, getContext().getAgent(comment.getAgentId())));
			}
			
			JSONObject response = new JSONObject();
			response.put("id",thread.getId());
			response.put("isAdmin", getContext().hasAccess(thread.getPermissions().owner) );
			response.put("isWriter", getContext().hasAccess(thread.getPermissions().owner) || getContext().hasAccess(thread.getPermissions().writer) );
			response.put("comments", list);
			
			return new HttpResponse(response.toJSONString(), HttpURLConnection.HTTP_OK);
		}
		catch (AgentLockedException | PermissionException e) {
			return new HttpResponse("Forbidden", HttpURLConnection.HTTP_FORBIDDEN);
		}
		catch (StorageException | AgentNotKnownException e) {
			e.printStackTrace();
			return new HttpResponse("Internal Server Error", HttpURLConnection.HTTP_INTERNAL_ERROR);
		}
		catch (NotFoundException e) {
			return new HttpResponse("Not Found", HttpURLConnection.HTTP_NOT_FOUND);
		}
	}
	
	/**
	 * Create new comment
	 * @param parentId Id of the parent
	 * @param body Comment body
	 * @return
	 */
	@POST
	@Path("/threads/{id}")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_CREATED, message = "Comment Id"),
			@ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "Forbidden"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Not Found"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal Server Error")
	})
	@ApiOperation(value = "createComment",
			notes = "Create a new Comment")
	public HttpResponse createComment(@PathParam("id") String parentId, @ContentParam String body) {
		try {
			Comment comment = new Comment(getContext().getMainAgent().getId(),new Date(),body);
			_getCommentThread(parentId).addComment(comment);
			return new HttpResponse(comment.getId(), HttpURLConnection.HTTP_CREATED);
		}
		catch (PermissionException e) {
			return new HttpResponse("Forbidden", HttpURLConnection.HTTP_FORBIDDEN);
		}
		catch (StorageException e) {
			e.printStackTrace();
			return new HttpResponse("Internal Server Error", HttpURLConnection.HTTP_INTERNAL_ERROR);
		}
		catch (NotFoundException e) {
			return new HttpResponse("Not Found", HttpURLConnection.HTTP_NOT_FOUND);
		}
	}
	
	/**
	 * Create new comment (reply to another comment)
	 * @param parentId Id of the parent comment
	 * @param body Comment body
	 * @return
	 */
	@POST
	@Path("/comment/{id}/comments")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_CREATED, message = "Comment Id"),
			@ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "Forbidden"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Not Found"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal Server Error")
	})
	@ApiOperation(value = "createCommentReply",
			notes = "Create a new Comment (reply)")
	public HttpResponse createCommentReply(@PathParam("id") String parentId, @ContentParam String body) {
		try {
			Comment comment = new Comment(getContext().getMainAgent().getId(),new Date(),body);
			_getComment(parentId).addComment(comment);
			return new HttpResponse(comment.getId(), HttpURLConnection.HTTP_CREATED);
		}
		catch (PermissionException e) {
			return new HttpResponse("Forbidden", HttpURLConnection.HTTP_FORBIDDEN);
		}
		catch (StorageException e) {
			e.printStackTrace();
			return new HttpResponse("Internal Server Error", HttpURLConnection.HTTP_INTERNAL_ERROR);
		}
		catch (NotFoundException e) {
			return new HttpResponse("Not Found", HttpURLConnection.HTTP_NOT_FOUND);
		}
	}
	
	/**
	 * Get replys to another comment
	 * @param threadId Id of the thread
	 * @return 
	 */
	@GET
	@Path("/comment/{id}/comments")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Comment replys"),
			@ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "Forbidden"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Not Found"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal Server Error")
	})
	@ApiOperation(value = "getCommentReplys",
			notes = "Get comment thread including comments")
	public HttpResponse getCommentReplys(@PathParam("id") String commentId) {
		try {
			Comment mainComment = _getComment(commentId);
			List<Comment> comments = mainComment.getComments();
			
			JSONArray list = new JSONArray();
			for(Comment comment : comments) {
				list.add(_serializeComment(comment, getContext().getAgent(comment.getAgentId())));
			}
			
			JSONObject response = new JSONObject();
			response.put("id",mainComment.getId());
			response.put("comments", list);
			
			return new HttpResponse(response.toJSONString(), HttpURLConnection.HTTP_OK);
		}
		catch (PermissionException e) {
			return new HttpResponse("Forbidden", HttpURLConnection.HTTP_FORBIDDEN);
		}
		catch (StorageException | AgentNotKnownException e) {
			e.printStackTrace();
			return new HttpResponse("Internal Server Error", HttpURLConnection.HTTP_INTERNAL_ERROR);
		}
		catch (NotFoundException e) {
			return new HttpResponse("Not Found", HttpURLConnection.HTTP_NOT_FOUND);
		}
	}
	
	/**
	 * Get a comment by id
	 * @param id Id of the comment
	 * @return
	 */
	@GET
	@Path("/comment/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Comment"),
			@ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "Forbidden"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Not Found"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal Server Error")
	})
	@ApiOperation(value = "Comment",
			notes = "Get a comment by id.")
	public HttpResponse getComment(@PathParam("id") String id) {
		try {
			Comment comment = _getComment(id);
			
			return new HttpResponse(_serializeComment(comment,getContext().getAgent(comment.getAgentId())).toJSONString(), HttpURLConnection.HTTP_OK);
			
		}
		catch (PermissionException e) {
			return new HttpResponse("Forbidden", HttpURLConnection.HTTP_FORBIDDEN);
		}
		catch (StorageException | AgentNotKnownException e) {
			e.printStackTrace();
			return new HttpResponse("Internal Server Error", HttpURLConnection.HTTP_INTERNAL_ERROR);
		}
		catch (NotFoundException e) {
			return new HttpResponse("Not Found", HttpURLConnection.HTTP_NOT_FOUND);
		}
	}
	
	/**
	 * Edit a comment
	 * @param id comment id
	 * @param body comment body
	 * @return
	 */
	@PUT
	@Path("/comment/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Updated"),
			@ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "Forbidden"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Not Found"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal Server Error")
	})
	@ApiOperation(value = "editComment",
			notes = "Edit a Comment")
	public HttpResponse editComment(@PathParam("id") String id, @ContentParam  String body) {
		try {
			Comment comment = _getComment(id);
			comment.setBody(body);
			return new HttpResponse(_serializeComment(comment,(UserAgent)getContext().getAgent(comment.getAgentId())).toJSONString(), HttpURLConnection.HTTP_OK);
		}
		catch (PermissionException e) {
			return new HttpResponse("Forbidden", HttpURLConnection.HTTP_FORBIDDEN);
		}
		catch (StorageException | AgentNotKnownException e) {
			e.printStackTrace();
			return new HttpResponse("Internal Server Error", HttpURLConnection.HTTP_INTERNAL_ERROR);
		}
		catch (NotFoundException e) {
			return new HttpResponse("Not Found", HttpURLConnection.HTTP_NOT_FOUND);
		}
	}
	
	/**
	 * Deletes a comment
	 * @param id comment id
	 * @return
	 */
	@DELETE
	@Path("/comment/{id}")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Deleted resource"),
			@ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "Forbidden"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Not Found"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal Server Error")
	})
	@ApiOperation(value = "deleteComment",
			notes = "Delete a Comment")
	public HttpResponse deleteComment(@PathParam("id") String id) {
		try {
			Comment comment = _getComment(id);
			String response = _serializeComment(comment,getContext().getAgent(comment.getAgentId())).toJSONString();
			comment.delete();
			return new HttpResponse(response, HttpURLConnection.HTTP_OK);
		}
		catch (PermissionException e) {
			return new HttpResponse("Forbidden", HttpURLConnection.HTTP_FORBIDDEN);
		}
		catch (NotFoundException e) {
			return new HttpResponse("Not Found", HttpURLConnection.HTTP_NOT_FOUND);
		}
		catch (StorageException | AgentNotKnownException e) {
			e.printStackTrace();
			return new HttpResponse("Internal Server Error", HttpURLConnection.HTTP_INTERNAL_ERROR);
		}
	}
	
	/**
	 * Submit a vote for a comment
	 * @param commentId comment id
	 * @param body "true" for upvote, "false" for downvote
	 * @return
	 */
	@POST
	@Path("/comment/{id}/votes")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_CREATED, message = "Vote submitted"),
			@ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "Forbidden"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Not Found"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal Server Error")
	})
	@ApiOperation(value = "addVote",
			notes = "Add a vote")
	public HttpResponse addVote(@PathParam("id") String commentId, @ContentParam String body) {
		try {
			Comment c = _getComment(commentId);
			c.vote(getContext().getMainAgent().getId(),body.equals("true"));
			
			JSONObject response = new JSONObject();
			response.put("rating", c.getRating());
			
			return new HttpResponse(response.toJSONString(), HttpURLConnection.HTTP_CREATED);
		}
		catch (PermissionException e) {
			return new HttpResponse("Forbidden", HttpURLConnection.HTTP_FORBIDDEN);
		}
		catch (StorageException e) {
			e.printStackTrace();
			return new HttpResponse("Internal Server Error", HttpURLConnection.HTTP_INTERNAL_ERROR);
		}
		catch (NotFoundException e) {
			return new HttpResponse("Not Found", HttpURLConnection.HTTP_NOT_FOUND);
		}
	}
	
	
	// //////////////////////////////////////////////////////////////////////////////////////
	// Methods required by the LAS2peer framework.
	// //////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Method for debugging purposes.
	 * Here the concept of restMapping validation is shown.
	 * It is important to check, if all annotations are correct and consistent.
	 * Otherwise the service will not be accessible by the WebConnector.
	 * Best to do it in the unit tests.
	 * To avoid being overlooked/ignored the method is implemented here and not in the test section.
	 * @return  true, if mapping correct
	 */
	public boolean debugMapping() {
		String XML_LOCATION = "./restMapping.xml";
		String xml = getRESTMapping();

		try {
			RESTMapper.writeFile(XML_LOCATION, xml);
		} catch (IOException e) {
			e.printStackTrace();
		}

		XMLCheck validator = new XMLCheck();
		ValidationResult result = validator.validate(xml);

		if (result.isValid()) {
			return true;
		}
		return false;
	}

	/**
	 * This method is needed for every RESTful application in LAS2peer. There is no need to change!
	 * 
	 * @return the mapping
	 */
	public String getRESTMapping() {
		String result = "";
		try {
			result = RESTMapper.getMethodsAsXML(this.getClass());
		} catch (Exception e) {

			e.printStackTrace();
		}
		return result;
	}

}
