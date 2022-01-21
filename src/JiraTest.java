import static io.restassured.RestAssured.given;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;

import io.restassured.RestAssured;
import io.restassured.filter.session.SessionFilter;
import io.restassured.path.json.JsonPath;

public class JiraTest {

	public static void main(String[] args) {
		
		RestAssured.baseURI = "http://localhost:8080";

		// LOGIN
		System.out.println("#-#-#-#-#-# LOGIN #-#-#-#-#-#");
		// Should be used when session cookie is required
		SessionFilter session = new SessionFilter();
		
		given().relaxedHTTPSValidation() // HTTPS Authentication
		.header("Content-Type","application/json")
		.body("{ \"username\": \"nhk\", \"password\": \"1234\" }")
		.filter(session) // It's filled with session data from response
		.when().post("/rest/auth/1/session")
		.then().assertThat().statusCode(200).log().all()
		.extract().response().asString();
		
		// CREATE ISSUE
		/*
		System.out.println("#-#-#-#-#-# CREATE ISSUE #-#-#-#-#-#");
		String response = given().header("Content-Type","application/json").filter(session)
		.body("{\r\n"
				+ "    \"fields\": {\r\n"
				+ "        \"project\": {\r\n"
				+ "            \"key\": \"RES\"\r\n"
				+ "        },\r\n"
				+ "        \"summary\": \"Apple Bug\",\r\n"
				+ "        \"description\": \"Testing Bug!\",\r\n"
				+ "        \"issuetype\": {\r\n"
				+ "            \"name\": \"Bug\"\r\n"
				+ "        }\r\n"
				+ "\r\n"
				+ "    }\r\n"
				+ "}")
		.when().post("/rest/api/2/issue")
		.then().assertThat().statusCode(201).log().all()
		.extract().response().asString();

		// Example using JsonPath to parse to JSON Object
		JsonPath js = new JsonPath(response);
		String issueId = js.getString("id");
		 */
		String issueId = "10208";
		
		//  ADD COMMENT
		System.out.println("#-#-#-#-#-# ADD COMMENT #-#-#-#-#-#");
		// pathParams id is used in .post(...{id}...)
		String commentMessage = "Random comment 1234!";
		String commentDetails = given().pathParams("id", issueId)
		.header("Content-Type","application/json").filter(session)
		.body("{\r\n"
				+ "    \"body\": \""+ commentMessage +"\",\r\n"
				+ "    \"visibility\": {\r\n"
				+ "        \"type\": \"role\",\r\n"
				+ "        \"value\": \"Administrators\"\r\n"
				+ "    }\r\n"
				+ "}")
		.when().post("/rest/api/2/issue/{id}/comment")
		.then().assertThat().statusCode(201).log().all()
		.extract().response().asString();
		
		JsonPath js = new JsonPath(commentDetails);
		String commentId = js.getString("id");
		
		// ADD ATTACHMENT
		System.out.println("#-#-#-#-#-# ADD ATTACHMENT #-#-#-#-#-#");
		Map<String, String> headers = new HashMap<String, String>(); // MULTIPLE HEADERS
		headers.put("X-Atlassian-Token", "no-check");
		headers.put("Content-Type", "multipart/form-data");
		
		given().pathParam("issueId", issueId).filter(session)
		.headers(headers)
		.multiPart("file", new File("attachment.txt")) // It should be sent as file class object in the multiPart method
		.when().post("/rest/api/2/issue/{issueId}/attachments")
		.then().log().all().assertThat().statusCode(200);
		
		// GET ISSUE
		System.out.println("#-#-#-#-#-# GET ISSUE #-#-#-#-#-#");
		String issueDetails = given().filter(session)
				.pathParam("issueId", issueId) // identify a specific resource[s]
				.param("fields", "comment") // filter those resources
		.when().get("/rest/api/2/issue/{issueId}")
		.then().assertThat().statusCode(200).extract().response().asString();
		
		System.out.println(issueDetails);
		js = new JsonPath(issueDetails);
		
		for (int i = 0; i < js.getInt("fields.comment.comments.size()"); i++) {
			
			String id = js.getString("fields.comment.comments["+i+"].id");
			String message = js.getString("fields.comment.comments["+i+"].body");
			
			if(commentId.equals(id))
			{
				Assert.assertEquals(commentMessage, message);
				break;
			}
		}
		
	}
}
