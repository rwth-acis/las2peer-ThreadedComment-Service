package i5.las2peer.services.threadedCommentService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.connectors.webConnector.client.ClientResponse;
import i5.las2peer.connectors.webConnector.client.MiniClient;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.LocalNodeManager;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.services.commentManagementService.CommentManagementService;
import i5.las2peer.testing.MockAgentFactory;

/**
 * Tests both services
 *
 */
public class ServiceTest {

	private static final String HTTP_ADDRESS = "http://127.0.0.1";
	private static int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;

	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;

	private static UserAgentImpl agentAdam;
	private static UserAgentImpl agentEve;
	private static UserAgentImpl agentAbel;
	private static UserAgentImpl agentKlaus;
	private static final String passAdam = "adamspass";
	private static final String passEve = "evespass";
	private static final String passAbel = "abelspass";
	private static final String passKlaus = "klauspass";

	private static final ServiceNameVersion testCommentService = new ServiceNameVersion(
			ThreadedCommentService.class.getCanonicalName(), "0.2.0");
	private static final ServiceNameVersion testCommentManagementService = new ServiceNameVersion(
			CommentManagementService.class.getCanonicalName(), "0.2.0");

	private static final String mainPath = "comments/";
	private static final String mainPathManager = "commentmanagement/";

	private static int getUnusedPort() {
		int port = HTTP_PORT;
		try {
			ServerSocket socket = new ServerSocket(0);
			port = socket.getLocalPort();
			socket.close();
		} catch (IOException e) {
		}
		return port;
	}

	/**
	 * Called before the tests start.
	 * 
	 * Sets up the node and initializes connector and users that can be used throughout the tests.
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	public static void startServer() throws Exception {
		// get unsued port
		HTTP_PORT = getUnusedPort();

		// start node
		node = new LocalNodeManager().newNode();
		agentAdam = MockAgentFactory.getAdam();
		agentAdam.unlock(passAdam);
		agentEve = MockAgentFactory.getEve();
		agentEve.unlock(passEve);
		agentAbel = MockAgentFactory.getAbel();
		agentAbel.unlock(passAbel);
		agentKlaus = UserAgentImpl.createUserAgent(passKlaus);
		agentKlaus.unlock(passKlaus);
		node.storeAgent(agentAdam);
		node.storeAgent(agentEve);
		node.storeAgent(agentAbel);
		node.storeAgent(agentKlaus);
		node.launch();

		ServiceAgentImpl testService = ServiceAgentImpl.createServiceAgent(testCommentService, "a pass");
		testService.unlock("a pass");

		node.registerReceiver(testService);

		ServiceAgentImpl testServiceExample = ServiceAgentImpl.createServiceAgent(testCommentManagementService,
				"a pass");
		testServiceExample.unlock("a pass");

		node.registerReceiver(testServiceExample);

		// start connector
		logStream = new ByteArrayOutputStream();
		connector = new WebConnector(true, HTTP_PORT, false, 1000);
		connector.setLogStream(new PrintStream(logStream));
		connector.start(node);

		Thread.sleep(1000); // wait a second for the connector to become ready

		agentAdam = MockAgentFactory.getAdam();
		agentEve = MockAgentFactory.getEve();
		agentAbel = MockAgentFactory.getAbel();
		agentKlaus.lockPrivateKey();
	}

	/**
	 * Called after the tests have finished. Shuts down the server and prints out the connector log file for reference.
	 * 
	 * @throws Exception
	 */
	@AfterClass
	public static void shutDownServer() throws Exception {

		connector.stop();
		node.shutDown();

		connector = null;
		node = null;

		System.out.println("Connector-Log:");
		System.out.println("--------------");

		System.out.println(logStream.toString());

	}

	/**
	 * Test CommentService
	 * 
	 */
	@Test
	public void testMethods() {
		try {
			// create clients
			MiniClient cAdam = new MiniClient();
			String endpoint = connector.getHttpEndpoint();
			cAdam.setConnectorEndpoint(endpoint);
			cAdam.setLogin(agentAdam.getIdentifier(), passAdam);

			MiniClient cEve = new MiniClient();
			cEve.setConnectorEndpoint(endpoint);
			cEve.setLogin(agentEve.getIdentifier(), passEve);

			MiniClient cAbel = new MiniClient();
			cAbel.setConnectorEndpoint(endpoint);
			cAbel.setLogin(agentAbel.getIdentifier(), passAbel);

			MiniClient cKlaus = new MiniClient();
			cKlaus.setConnectorEndpoint(endpoint);
			cKlaus.setLogin(agentKlaus.getIdentifier(), passKlaus);

			MiniClient cAnonymous = new MiniClient();
			cAnonymous.setConnectorEndpoint(endpoint);

			// create comment thread
			ClientResponse result = cAdam.sendRequest("POST", mainPathManager + "threads",
					"{owner:" + agentAdam.getLoginName() + ",writer:" + agentEve.getLoginName() + ",reader:"
							+ agentAbel.getLoginName() + "}");
			assertEquals(201, result.getHttpCode());
			String threadId = result.getResponse().trim();

			System.out.println("CreateThread: " + threadId);

			// get comment thread
			ClientResponse result1 = cAdam.sendRequest("GET", mainPath + "threads/" + threadId, "");
			assertEquals(200, result1.getHttpCode());

			System.out.println("GetCommentThread: " + result1.getResponse().trim());

			// add comment
			ClientResponse result2 = cEve.sendRequest("POST", mainPath + "threads/" + threadId, "comment1_content");
			assertEquals(201, result2.getHttpCode());
			String commentId = result2.getResponse().trim();

			System.out.println("AddComment: " + commentId);

			// add comment without permission
			ClientResponse result3 = cAbel.sendRequest("POST", mainPath + "threads/" + threadId,
					"comment_evil_content");
			assertEquals(403, result3.getHttpCode());
			assertTrue(result3.getResponse().contains("Forbidden"));

			System.out.println("AddComment (Evil): " + result3.getResponse().trim());

			// get comment thread
			ClientResponse result4 = cAdam.sendRequest("GET", mainPath + "threads/" + threadId, "");
			assertEquals(200, result4.getHttpCode());
			assertTrue(result4.getResponse().contains("comment1_content"));
			assertFalse(result4.getResponse().contains("comment_evil_content"));

			System.out.println("GetCommentThread: " + result4.getResponse().trim());

			// get comment thread without permission
			ClientResponse result5 = cKlaus.sendRequest("GET", mainPath + "threads/" + threadId, "");
			System.out.println(result5.getResponse().trim());
			assertEquals(403, result5.getHttpCode());
			assertTrue(result5.getResponse().trim().contains("Forbidden"));

			System.out.println("GetCommentThread (Evil): " + result5.getResponse().trim());

			// edit comment
			ClientResponse result6 = cEve.sendRequest("PUT", mainPath + "comment/" + commentId,
					"comment1_content_edited");
			assertEquals(200, result6.getHttpCode());
			assertTrue(result6.getResponse().trim().contains("comment1_content_edited"));

			System.out.println("EditComment: " + result6.getResponse().trim());

			// edit comment without permissions
			ClientResponse result7 = cAbel.sendRequest("PUT", mainPath + "comment/" + commentId,
					"comment1_content_edited_evil");
			assertEquals(403, result7.getHttpCode());
			assertFalse(result7.getResponse().trim().contains("comment1_content_edited_evil"));

			System.out.println("EditComment (Evil): " + result7.getResponse().trim());

			// vote comment
			ClientResponse result8 = cEve.sendRequest("POST", mainPath + "comment/" + commentId + "/votes", "true");
			assertEquals(201, result8.getHttpCode());
			assertTrue(result8.getResponse().trim().contains("1"));

			System.out.println("AddVote: " + result8.getResponse().trim());

			// vote comment without permissions
			ClientResponse result9 = cAbel.sendRequest("POST", mainPath + "comment/" + commentId + "/votes", "false");
			assertEquals(403, result9.getHttpCode());
			assertTrue(result9.getResponse().trim().contains("Forbidden"));

			System.out.println("AddVote (Evil): " + result9.getResponse().trim());

			// reply to comment
			ClientResponse result10 = cEve.sendRequest("POST", mainPath + "comment/" + commentId + "/comments",
					"comment2_content");
			assertEquals(201, result10.getHttpCode());
			String replyId = result10.getResponse().trim();

			System.out.println("AddCommentReply: " + replyId);

			// reply to comment without permissions
			ClientResponse result11 = cAbel.sendRequest("POST", mainPath + "comment/" + commentId + "/comments",
					"comment2_content_evil");
			assertEquals(403, result11.getHttpCode());
			assertTrue(result11.getResponse().trim().contains("Forbidden"));

			System.out.println("AddCommentReply (Evil): " + result11.getResponse().trim());

			// get comment
			ClientResponse result12 = cAdam.sendRequest("GET", mainPath + "comment/" + commentId, "");
			assertEquals(200, result12.getHttpCode());
			assertTrue(result12.getResponse().contains("comment1_content_edited"));
			assertFalse(result12.getResponse().contains("evil"));
			assertTrue(result12.getResponse().contains("\"replyCount\":1"));

			System.out.println("GetComment: " + result12.getResponse().trim());

			// get replys of comment
			ClientResponse result13 = cAdam.sendRequest("GET", mainPath + "comment/" + commentId + "/comments", "");
			assertEquals(200, result13.getHttpCode());
			assertTrue(result13.getResponse().contains("comment2_content"));
			assertFalse(result13.getResponse().contains("evil"));

			System.out.println("GetCommentReplys: " + result13.getResponse().trim());

			// delete comment without permissions
			ClientResponse result14 = cAbel.sendRequest("DELETE", mainPath + "comment/" + replyId, "");
			assertEquals(403, result14.getHttpCode());
			assertTrue(result14.getResponse().contains("Forbidden"));

			System.out.println("DeleteComment (Evil): " + result14.getResponse().trim());

			// delete comment
			ClientResponse result15 = cAdam.sendRequest("DELETE", mainPath + "comment/" + replyId, "");
			assertEquals(200, result15.getHttpCode());
			assertTrue(result15.getResponse().contains(replyId));

			System.out.println("DeleteComment: " + result15.getResponse().trim());

			// delete comment thread
			ClientResponse result17 = cAdam.sendRequest("DELETE", mainPathManager + "threads/" + threadId, "");
			assertEquals(200, result17.getHttpCode());

			System.out.println("DeleteThread: " + result17.getResponse().trim());

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

}
