package i5.las2peer.services.threadedCommentService;

import i5.las2peer.api.Context;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.services.threadedCommentService.data.Comment;
import i5.las2peer.services.threadedCommentService.data.CommentThread;
import i5.las2peer.services.threadedCommentService.data.Permissions;
import i5.las2peer.services.threadedCommentService.storage.Storage;
import i5.las2peer.services.threadedCommentService.storage.NotFoundException;
import i5.las2peer.services.threadedCommentService.storage.PermissionException;
import i5.las2peer.services.threadedCommentService.storage.StorageException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;

import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

/**
 * las2peer Threaded Comment Service
 * 
 * A threaded comment service. Other services can create a new thread and set up permissions. Users can post comments to
 * this thread, reply to other comments and up/dwonvote comments.
 * 
 * @author Jasper Nalbach
 *
 */
@ServicePath("/comments")
public class ThreadedCommentService extends RESTService {

	// TODO make use of Jersey features
	
	// TODO refactor storage

	/**
	 * Create a new storage for the current context
	 * 
	 * @return A Storage with the current context
	 * @throws StorageException
	 */
	private Storage getStorage() throws StorageException {
		return new Storage();
	}

	// helper

	private CommentThread _getCommentThread(String id) throws StorageException, PermissionException, NotFoundException {
		return (CommentThread) getStorage().load(id);
	}

	private Comment _getComment(String id) throws StorageException, PermissionException, NotFoundException {
		return (Comment) getStorage().load(id);
	}

	private JSONObject _serializeComment(Comment comment, Agent author) throws StorageException, PermissionException {
		JSONObject json = new JSONObject();

		json.put("id", comment.getId());
		json.put("author", _serializeAuthor(author));
		json.put("date", (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(comment.getDate()));
		json.put("body", comment.getBody());
		json.put("upvotes", comment.getUpvotes());
		json.put("downvotes", comment.getDownvotes());
		json.put("myRating", comment.getVote(Context.get().getMainAgent().getIdentifier()));
		json.put("replyCount", comment.getCommentCount());

		return json;
	}

	private JSONObject _serializeAuthor(Agent agent) {
		JSONObject json = new JSONObject();

		if (agent instanceof UserAgent)
			json.put("name", ((UserAgent) agent).getLoginName());
		else
			json.put("name", "Unknown");

		json.put("id", agent.getIdentifier());
		json.put("isMe", agent.equals(Context.get().getMainAgent()));

		return json;
	}

	// //////////////////////////////////////////////////////////////////////////////////////
	// Service methods.
	// //////////////////////////////////////////////////////////////////////////////////////

	// RMI

	/**
	 * Create a new comment thread.
	 * 
	 * @param owner Id of the owning agent. This agent is able to administer comments.
	 * @param writer Id of the agent who is able to post new comments and up/downvote them.
	 * @param reader Id of the agent with read-only access.
	 * @return Id of the comment thread to be stored by the using serivce.
	 */
	public String createCommentThread(String owner, String writer, String reader) {
		try {
			CommentThread thread = getStorage().init(new CommentThread(new Permissions(owner, writer, reader)));
			return thread.getId();
		} catch (Exception e) {
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
			return false;
		}

	}

	@Api
	@SwaggerDefinition(
			info = @Info(
					title = "las2peer Threaded Comment Service",
					version = "0.1",
					description = "A las2peer Service providing threaded comment functionality intended to be integrated with other services.",
					termsOfService = "",
					contact = @Contact(
							name = "Jasper Nalbach",
							url = "las2peer.org",
							email = "nalbach@dbis.rwth-aachen.de"),
					license = @License(
							name = "MIT",
							url = "")))
	@Path("/")
	public static class RootResource {
		private ThreadedCommentService service = (ThreadedCommentService) Context.getCurrent().getService();

		/**
		 * Get the comment thread including comments.
		 * 
		 * @param threadId Id of the thread
		 * @return
		 */
		@GET
		@Path("/threads/{id}")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "Comment Thread"), @ApiResponse(
						code = HttpURLConnection.HTTP_FORBIDDEN,
						message = "Forbidden"), @ApiResponse(
						code = HttpURLConnection.HTTP_NOT_FOUND,
						message = "Not Found"), @ApiResponse(
						code = HttpURLConnection.HTTP_INTERNAL_ERROR,
						message = "Internal Server Error") })
		@ApiOperation(
				value = "getCommentThread",
				notes = "Get comment thread including comments")
		public Response getCommentThread(@PathParam("id") String threadId) {
			try {
				CommentThread thread = service._getCommentThread(threadId);
				List<Comment> comments = thread.getComments();

				JSONArray list = new JSONArray();
				for (Comment comment : comments) {
					list.add(service._serializeComment(comment, Context.get().fetchAgent(comment.getAgentId())));
				}

				JSONObject response = new JSONObject();
				response.put("id", thread.getId());
				response.put("isAdmin", Context.getCurrent().hasAccess(thread.getPermissions().owner));
				response.put("isWriter", Context.getCurrent().hasAccess(thread.getPermissions().owner)
						|| Context.getCurrent().hasAccess(thread.getPermissions().writer));
				response.put("comments", list);

				return Response.ok().entity(response.toJSONString()).build();
			} catch (PermissionException e) {
				e.printStackTrace();
				return Response.status(Status.FORBIDDEN).entity("Forbidden").build();
			} catch (NotFoundException e) {
				e.printStackTrace();
				return Response.status(Status.NOT_FOUND).entity("Not Found").build();
			} catch (Exception e) {
				e.printStackTrace();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Internal Server Error").build();
			} 
		}

		/**
		 * Create new comment
		 * 
		 * @param parentId Id of the parent
		 * @param body Comment body
		 * @return
		 */
		@POST
		@Path("/threads/{id}")
		@Produces(MediaType.TEXT_PLAIN)
		@Consumes(MediaType.TEXT_PLAIN)
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_CREATED,
						message = "Comment Id"), @ApiResponse(
						code = HttpURLConnection.HTTP_FORBIDDEN,
						message = "Forbidden"), @ApiResponse(
						code = HttpURLConnection.HTTP_NOT_FOUND,
						message = "Not Found"), @ApiResponse(
						code = HttpURLConnection.HTTP_INTERNAL_ERROR,
						message = "Internal Server Error") })
		@ApiOperation(
				value = "createComment",
				notes = "Create a new Comment")
		public Response createComment(@PathParam("id") String parentId, String body) {
			try {
				Comment comment = new Comment(Context.getCurrent().getMainAgent().getIdentifier(), new Date(), body);
				service._getCommentThread(parentId).addComment(comment);
				return Response.status(Status.CREATED).entity(comment.getId()).build();
			} catch (PermissionException e) {
				e.printStackTrace();
				return Response.status(Status.FORBIDDEN).entity("Forbidden").build();
			} catch (StorageException e) {
				e.printStackTrace();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Internal Server Error").build();
			} catch (NotFoundException e) {
				e.printStackTrace();
				return Response.status(Status.NOT_FOUND).entity("Not Found").build();
			}
		}

		/**
		 * Create new comment (reply to another comment)
		 * 
		 * @param parentId Id of the parent comment
		 * @param body Comment body
		 * @return
		 */
		@POST
		@Path("/comment/{id}/comments")
		@Produces(MediaType.TEXT_PLAIN)
		@Consumes(MediaType.TEXT_PLAIN)
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_CREATED,
						message = "Comment Id"), @ApiResponse(
						code = HttpURLConnection.HTTP_FORBIDDEN,
						message = "Forbidden"), @ApiResponse(
						code = HttpURLConnection.HTTP_NOT_FOUND,
						message = "Not Found"), @ApiResponse(
						code = HttpURLConnection.HTTP_INTERNAL_ERROR,
						message = "Internal Server Error") })
		@ApiOperation(
				value = "createCommentReply",
				notes = "Create a new Comment (reply)")
		public Response createCommentReply(@PathParam("id") String parentId, String body) {
			try {
				Comment comment = new Comment(Context.getCurrent().getMainAgent().getIdentifier(), new Date(), body);
				service._getComment(parentId).addComment(comment);
				return Response.status(Status.CREATED).entity(comment.getId()).build();
			} catch (PermissionException e) {
				e.printStackTrace();
				return Response.status(Status.FORBIDDEN).entity("Forbidden").build();
			} catch (StorageException e) {
				e.printStackTrace();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Internal Server Error").build();
			} catch (NotFoundException e) {
				e.printStackTrace();
				return Response.status(Status.NOT_FOUND).entity("Not Found").build();
			}
		}

		/**
		 * Get replys to another comment
		 * 
		 * @param commentId
		 * @return
		 */
		@GET
		@Path("/comment/{id}/comments")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "Comment replys"), @ApiResponse(
						code = HttpURLConnection.HTTP_FORBIDDEN,
						message = "Forbidden"), @ApiResponse(
						code = HttpURLConnection.HTTP_NOT_FOUND,
						message = "Not Found"), @ApiResponse(
						code = HttpURLConnection.HTTP_INTERNAL_ERROR,
						message = "Internal Server Error") })
		@ApiOperation(
				value = "getCommentReplys",
				notes = "Get comment thread including comments")
		public Response getCommentReplys(@PathParam("id") String commentId) {
			try {
				Comment mainComment = service._getComment(commentId);
				List<Comment> comments = mainComment.getComments();

				JSONArray list = new JSONArray();
				for (Comment comment : comments) {
					list.add(service._serializeComment(comment, Context.getCurrent().fetchAgent(comment.getAgentId())));
				}

				JSONObject response = new JSONObject();
				response.put("id", mainComment.getId());
				response.put("comments", list);

				return Response.ok().entity(response.toJSONString()).build();
			} catch (PermissionException e) {
				e.printStackTrace();
				return Response.status(Status.FORBIDDEN).entity("Forbidden").build();
			} catch (NotFoundException e) {
				e.printStackTrace();
				return Response.status(Status.NOT_FOUND).entity("Not Found").build();
			} catch (Exception e) {
				e.printStackTrace();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Internal Server Error").build();
			} 
		}

		/**
		 * Get a comment by id
		 * 
		 * @param id Id of the comment
		 * @return
		 */
		@GET
		@Path("/comment/{id}")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "Comment"), @ApiResponse(
						code = HttpURLConnection.HTTP_FORBIDDEN,
						message = "Forbidden"), @ApiResponse(
						code = HttpURLConnection.HTTP_NOT_FOUND,
						message = "Not Found"), @ApiResponse(
						code = HttpURLConnection.HTTP_INTERNAL_ERROR,
						message = "Internal Server Error") })
		@ApiOperation(
				value = "Comment",
				notes = "Get a comment by id.")
		public Response getComment(@PathParam("id") String id) {
			try {
				Comment comment = service._getComment(id);

				return Response
						.ok()
						.entity(service._serializeComment(comment, Context.getCurrent().fetchAgent(comment.getAgentId()))
								.toJSONString()).build();

			} catch (PermissionException e) {
				e.printStackTrace();
				return Response.status(Status.FORBIDDEN).entity("Forbidden").build();
			} catch (NotFoundException e) {
				e.printStackTrace();
				return Response.status(Status.NOT_FOUND).entity("Not Found").build();
			} catch (Exception e) {
				e.printStackTrace();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Internal Server Error").build();
			} 
		}

		/**
		 * Edit a comment
		 * 
		 * @param id comment id
		 * @param body comment body
		 * @return
		 */
		@PUT
		@Path("/comment/{id}")
		@Produces(MediaType.APPLICATION_JSON)
		@Consumes(MediaType.TEXT_PLAIN)
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "Updated"), @ApiResponse(
						code = HttpURLConnection.HTTP_FORBIDDEN,
						message = "Forbidden"), @ApiResponse(
						code = HttpURLConnection.HTTP_NOT_FOUND,
						message = "Not Found"), @ApiResponse(
						code = HttpURLConnection.HTTP_INTERNAL_ERROR,
						message = "Internal Server Error") })
		@ApiOperation(
				value = "editComment",
				notes = "Edit a Comment")
		public Response editComment(@PathParam("id") String id, String body) {
			try {
				Comment comment = service._getComment(id);
				comment.setBody(body);
				return Response
						.ok()
						.entity(service._serializeComment(comment, Context.getCurrent().fetchAgent(comment.getAgentId()))
								.toJSONString()).build();
			} catch (PermissionException e) {
				e.printStackTrace();
				return Response.status(Status.FORBIDDEN).entity("Forbidden").build();
			} catch (NotFoundException e) {
				e.printStackTrace();
				return Response.status(Status.NOT_FOUND).entity("Not Found").build();
			} catch (Exception e) {
				e.printStackTrace();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Internal Server Error").build();
			} 
		}

		/**
		 * Deletes a comment
		 * 
		 * @param id comment id
		 * @return
		 */
		@DELETE
		@Path("/comment/{id}")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "Deleted resource"), @ApiResponse(
						code = HttpURLConnection.HTTP_FORBIDDEN,
						message = "Forbidden"), @ApiResponse(
						code = HttpURLConnection.HTTP_NOT_FOUND,
						message = "Not Found"), @ApiResponse(
						code = HttpURLConnection.HTTP_INTERNAL_ERROR,
						message = "Internal Server Error") })
		@ApiOperation(
				value = "deleteComment",
				notes = "Delete a Comment")
		public Response deleteComment(@PathParam("id") String id) {
			try {
				Comment comment = service._getComment(id);
				String response = service._serializeComment(comment,
						Context.getCurrent().fetchAgent(comment.getAgentId())).toJSONString();
				comment.delete();
				return Response.ok().entity(response).build();
			} catch (PermissionException e) {
				e.printStackTrace();
				return Response.status(Status.FORBIDDEN).entity("Forbidden").build();
			} catch (NotFoundException e) {
				e.printStackTrace();
				return Response.status(Status.NOT_FOUND).entity("Not Found").build();
			} catch (Exception e) {
				e.printStackTrace();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Internal Server Error").build();
			}
		}

		/**
		 * Submit a vote for a comment
		 * 
		 * @param commentId comment id
		 * @param body "true" for upvote, "false" for downvote
		 * @return
		 */
		@POST
		@Path("/comment/{id}/votes")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_CREATED,
						message = "Vote submitted"), @ApiResponse(
						code = HttpURLConnection.HTTP_FORBIDDEN,
						message = "Forbidden"), @ApiResponse(
						code = HttpURLConnection.HTTP_NOT_FOUND,
						message = "Not Found"), @ApiResponse(
						code = HttpURLConnection.HTTP_INTERNAL_ERROR,
						message = "Internal Server Error") })
		@ApiOperation(
				value = "addVote",
				notes = "Add a vote")
		@Consumes(MediaType.TEXT_PLAIN)
		public Response addVote(@PathParam("id") String commentId, String body) {
			try {
				Comment c = service._getComment(commentId);
				c.vote(Context.getCurrent().getMainAgent().getIdentifier(), body.equals("true"));

				JSONObject response = new JSONObject();
				response.put("upvotes", c.getUpvotes());
				response.put("downvotes", c.getDownvotes());

				return Response.status(Status.CREATED).entity(response.toJSONString()).build();
			} catch (PermissionException e) {
				e.printStackTrace();
				return Response.status(Status.FORBIDDEN).entity("Forbidden").build();
			} catch (StorageException e) {
				e.printStackTrace();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Internal Server Error").build();
			} catch (NotFoundException e) {
				e.printStackTrace();
				return Response.status(Status.NOT_FOUND).entity("Not Found").build();
			}
		}
	}

	@Override
	protected void initResources() {
		getResourceConfig().register(RootResource.class);
	}
}
