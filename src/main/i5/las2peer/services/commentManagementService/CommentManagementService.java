package i5.las2peer.services.commentManagementService;

import java.io.Serializable;
import java.net.HttpURLConnection;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import i5.las2peer.api.Context;
import i5.las2peer.api.persistency.Envelope;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.GroupAgent;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

/**
 * Comment Service Example Service
 * 
 * Example Service to show how to use the CommentService
 */
@ServicePath("/commentmanagement")
public class CommentManagementService extends RESTService {

	private static String containerIdentifier = "COMMENTEXAMPLESERVICE";
	private static String tcs = "i5.las2peer.services.threadedCommentService.ThreadedCommentService@0.2";

	// RMI methods

	public String createCommentThread(String owner, String writer, String reader) throws Exception {
		// invoke remote service method
		Object result = Context.get().invoke(tcs, "createCommentThread", new Serializable[] { owner, writer, reader });

		if (result != null) {
			return (String) result;
		}

		throw new Exception("Got null");
	}

	public boolean deleteCommentThread(String id) throws Exception {
		// invoke remote service method
		Object result = Context.get().invoke(tcs, "deleteCommentThread", new Serializable[] { id });

		if (result != null) {
			return (Boolean) result;
		}

		throw new Exception("Got null");
	}

	// Storage methods

	private Container fetchContainer() {
		try {
			Envelope env = Context.get().requestEnvelope(containerIdentifier);
			return (Container) env.getContent();
		} catch (Exception e) {
			System.err.println("Can't fetch from network storage!");
			return new Container();
		}
	}

	private void storeContainer(Container container) throws Exception {
		Envelope env;
		try {
			env = Context.get().requestEnvelope(containerIdentifier);
		} catch (EnvelopeNotFoundException e) {
			env = Context.get().createEnvelope(containerIdentifier);
			env.setPublic();
		}

		env.setContent(container);
		Context.get().storeEnvelope(env);
	}

	// Group Creation helper
	// TODO : group containing all users is not possible at the moment
	// TODO : this should be done using a seperate group management service
	private String createGroup(String agentString) throws Exception {
		String[] agentNameList = agentString.split(",");

		Agent[] agentList = new Agent[agentNameList.length + 1];
		agentList[0] = Context.get().getMainAgent();

		int i = 1;
		for (String agentName : agentNameList) {
			String agentId = Context.get().getUserAgentIdentifierByLoginName(agentName.trim());
			Agent agent = Context.get().fetchAgent(agentId);
			agentList[i] = agent;
			i++;
		}

		GroupAgent group = Context.get().createGroupAgent(agentList);
		Context.get().storeAgent(group);

		return group.getIdentifier();
	}

	// Service methods

	// TODO : per user thread list

	@Api
	@SwaggerDefinition(
			info = @Info(
					title = "las2peer Comment Management Service",
					version = "0.2",
					description = "Create and manage comment threads to integrate them into websites.",
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
		CommentManagementService service = (CommentManagementService) Context.getCurrent().getService();

		@GET
		@Path("/threads")
		@Produces(MediaType.TEXT_PLAIN)
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "List of threads"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_INTERNAL_ERROR,
								message = "Internal Server Error") })
		public Response getThreads() {
			List<String> threads = service.fetchContainer().getThreads();

			JSONArray response = new JSONArray();

			for (String s : threads) {
				response.add(s);
			}

			return Response.ok().entity(response.toJSONString()).build();
		}

		@POST
		@Path("/threads")
		@Produces(MediaType.TEXT_PLAIN)
		@Consumes(MediaType.TEXT_PLAIN)
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_CREATED,
						message = "Id"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_INTERNAL_ERROR,
								message = "Internal Server Error") })
		public Response createThread(String content) {
			try {
				JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
				JSONObject params = (JSONObject) parser.parse(content);

				String newThread = service.createCommentThread(service.createGroup((String) params.get("owner")),
						service.createGroup((String) params.get("writer")),
						service.createGroup((String) params.get("reader")));

				Container c = service.fetchContainer();
				c.getThreads().add(newThread);

				service.storeContainer(c);

				return Response.status(Status.CREATED).entity(newThread).build();

			} catch (Exception e) {
				e.printStackTrace();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Internal Server Error").build();
			}
		}

		@DELETE
		@Path("/threads/{id}")
		@Produces(MediaType.TEXT_PLAIN)
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "Deleted"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_INTERNAL_ERROR,
								message = "Internal Server Error") })
		public Response deleteThread(@PathParam("id") String id) {
			/*
			 * Please note that this is insecure. Everybody could delete a comment thread, because this service is also
			 * owner of the thread. I na real world example, here should come some permission checks.
			*/
			try {
				boolean response = service.deleteCommentThread(id);

				if (!response)
					throw new Exception("Got false");

				Container c = service.fetchContainer();
				c.getThreads().remove(id);
				service.storeContainer(c);

				return Response.ok().entity("Deleted").build();

			} catch (Exception e) {
				e.printStackTrace();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Internal Server Error").build();
			}
		}
	}

	@Override
	protected void initResources() {
		getResourceConfig().register(RootResource.class);
	}
}
