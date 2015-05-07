package com.versionone.apiclient.integration.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.versionone.DB.DateTime;
import com.versionone.Oid;
import com.versionone.apiclient.Asset;
import com.versionone.apiclient.Attachments;
import com.versionone.apiclient.MimeType;
import com.versionone.apiclient.Query;
import com.versionone.apiclient.Services;
import com.versionone.apiclient.V1Connector;
import com.versionone.apiclient.exceptions.V1Exception;
import com.versionone.apiclient.interfaces.IAssetType;
import com.versionone.apiclient.interfaces.IAttachments;
import com.versionone.apiclient.interfaces.IAttributeDefinition;
import com.versionone.apiclient.interfaces.IOperation;
import com.versionone.apiclient.services.QueryResult;

public class V1ConnectorCreatesTests {
	private final static String TEST_PROJECT_NAME = "Java SDK Integration Tests";
	private static Oid _testProjectId;
	private static String url = "http://localhost//VersionOne/";
	private static V1Connector connector;
	private static Services services;
	private static String accessToken = "1.yL3CcovObgbQnmMKP8PKTt3fo7A=";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

	}

	@Before
	public void setUp() throws Exception {
		services = new Services(V1Connector.withInstanceUrl(url).withUserAgentHeader("IntegrationTests", "1.0").withAccessToken(accessToken).build());
		IAssetType assetType = services.getMeta().getAssetType("Scope");
		IAttributeDefinition nameAttribute = assetType.getAttributeDefinition("Name");
		Oid projectId = services.getOid("Scope:0");
		Asset newAsset = services.createNew(assetType, projectId);
		newAsset.setAttributeValue(nameAttribute, TEST_PROJECT_NAME);
		services.save(newAsset);
		_testProjectId = newAsset.getOid().getMomentless();

	}

	public void init() {
		try {
			connector = V1Connector.withInstanceUrl(url).withUserAgentHeader("IntegrationTests", "1.0").withUsernameAndPassword("admin", "1234")
					.build();
		} catch (MalformedURLException | V1Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		services = new Services(connector);
	}

	// @Test
	public void createEpic() throws V1Exception {

		IAssetType epicType = services.getMeta().getAssetType("Epic");
		Asset newEpic = null;
		newEpic = services.createNew(epicType, _testProjectId);
		IAttributeDefinition nameAttribute = epicType.getAttributeDefinition("Name");
		String name = "Test Epic " + _testProjectId + " Create epic";
		newEpic.setAttributeValue(nameAttribute, name);
		services.save(newEpic);

		assertNotNull(newEpic.getOid());
	}

	// @Test
	public void createEpicWithNestedStoryTest() throws V1Exception {

		init();

		IAssetType epicType = services.getMeta().getAssetType("Epic");
		Asset newEpic = services.createNew(epicType, _testProjectId);
		IAttributeDefinition epicNameAttribute = epicType.getAttributeDefinition("Name");
		IOperation generateSubStoryOperation = epicType.getOperation("GenerateSubStory");
		String name = "Test Epic " + _testProjectId + " Create epic with nested story";
		newEpic.setAttributeValue(epicNameAttribute, name);

		services.save(newEpic);
		services.executeOperation(generateSubStoryOperation, newEpic.getOid());

		assertNotNull(newEpic);

		Query query = new Query(newEpic.getOid().getMomentless());
		IAttributeDefinition subsAttributeDefinition = epicType.getAttributeDefinition("Subs");
		query.getSelection().add(subsAttributeDefinition);
		QueryResult result = services.retrieve(query);

		assertEquals(1, result.getTotalAvaliable());
	}

	 @Test
	public void createStoryTest() throws V1Exception {

		init();

		IAssetType storyType = services.getMeta().getAssetType("Story");
		Asset newStory = services.createNew(storyType, _testProjectId);
		IAttributeDefinition nameAttribute = storyType.getAttributeDefinition("Name");
		String name = "Story ";
		newStory.setAttributeValue(nameAttribute, name);
		services.save(newStory);

		assertNotNull(newStory);
	}

	// @Test
	public void createStoryWithConversation() throws V1Exception {
		init();

		IAssetType storyType = services.getMeta().getAssetType("Story");
		Asset newStory = services.createNew(storyType, _testProjectId);
		IAttributeDefinition nameAttribute = storyType.getAttributeDefinition("Name");
		String name = "Test Story " + _testProjectId + " Create story with conversation";
		newStory.setAttributeValue(nameAttribute, name);
		services.save(newStory);

		IAssetType conversationType = services.getMeta().getAssetType("Conversation");
		IAssetType expressionType = services.getMeta().getAssetType("Expression");
		IAttributeDefinition authorAttribute = expressionType.getAttributeDefinition("Author");
		IAttributeDefinition authoredAtAttribute = expressionType.getAttributeDefinition("AuthoredAt");
		IAttributeDefinition contentAttribute = expressionType.getAttributeDefinition("Content");
		IAttributeDefinition belongsToAttribute = expressionType.getAttributeDefinition("BelongsTo");
		IAttributeDefinition inReplyToAttribute = expressionType.getAttributeDefinition("InReplyTo");
		IAttributeDefinition mentionsAttribute = expressionType.getAttributeDefinition("Mentions");
		Asset newConversation = services.createNew(conversationType, null);
		Asset questionExpression = services.createNew(expressionType, null);
		services.save(newConversation);

		questionExpression.setAttributeValue(authorAttribute, services.getOid("Member:20"));
		questionExpression.setAttributeValue(authoredAtAttribute, DateTime.now());
		questionExpression.setAttributeValue(contentAttribute, "Is this a test conversation?");
		questionExpression.setAttributeValue(belongsToAttribute, newConversation.getOid());
		questionExpression.addAttributeValue(mentionsAttribute, newStory.getOid());
		services.save(questionExpression);

		Query query = new Query(questionExpression.getOid().getMomentless());
		IAttributeDefinition subsAttributeDefinition = questionExpression.getAssetType().getAttributeDefinition("Content");
		query.getSelection().add(subsAttributeDefinition);
		QueryResult result = services.retrieve(query);

		assertEquals("Is this a test conversation?", result.getAssets()[0].getAttribute(contentAttribute).getValue().toString());

		Asset answerExpression = services.createNew(expressionType, questionExpression.getOid());
		answerExpression.setAttributeValue(authorAttribute, services.getOid("Member:20"));
		answerExpression.setAttributeValue(authoredAtAttribute, DateUtils.addMinutes(DateTime.now().getValue(), 15));
		answerExpression.setAttributeValue(contentAttribute, "Yes it is!");
		answerExpression.setAttributeValue(inReplyToAttribute, questionExpression.getOid());
		services.save(answerExpression);

		query = new Query(answerExpression.getOid().getMomentless());
		subsAttributeDefinition = answerExpression.getAssetType().getAttributeDefinition("Content");
		query.getSelection().add(subsAttributeDefinition);
		result = services.retrieve(query);

		assertEquals("Yes it is!", result.getAssets()[0].getAttribute(contentAttribute).getValue().toString());
		assertEquals(1, result.getTotalAvaliable());

	}

	// @Test
	public void createStoryWithConversationAndMentionTest() throws V1Exception {

		IAssetType storyType = services.getMeta().getAssetType("Story");
		Asset newStory = services.createNew(storyType, _testProjectId);
		IAttributeDefinition nameAttribute = storyType.getAttributeDefinition("Name");
		IAttributeDefinition mentionedInExpressionsAttribute = storyType.getAttributeDefinition("MentionedInExpressions");
		String name = "Test Story " + _testProjectId + " Create story with conversation and mention";
		newStory.setAttributeValue(nameAttribute, name);
		services.save(newStory);

		Asset storyTobeMentioned = services.createNew(storyType, _testProjectId);
		storyTobeMentioned.setAttributeValue(nameAttribute, name + " (to be mentioned)");
		services.save(storyTobeMentioned);

		IAssetType conversationType = services.getMeta().getAssetType("Conversation");
		IAssetType expressionType = services.getMeta().getAssetType("Expression");
		IAttributeDefinition authorAttribute = expressionType.getAttributeDefinition("Author");
		IAttributeDefinition authoredAtAttribute = expressionType.getAttributeDefinition("AuthoredAt");
		IAttributeDefinition contentAttribute = expressionType.getAttributeDefinition("Content");
		IAttributeDefinition belongsToAttribute = expressionType.getAttributeDefinition("BelongsTo");
		IAttributeDefinition inReplyToAttribute = expressionType.getAttributeDefinition("InReplyTo");

		Asset newConversation = services.createNew(conversationType, newStory.getOid());
		Asset questionExpression = services.createNew(expressionType, newStory.getOid());

		services.save(newConversation);

		questionExpression.setAttributeValue(authorAttribute, services.getOid("Member:20"));
		questionExpression.setAttributeValue(authoredAtAttribute, DateTime.now());
		questionExpression.setAttributeValue(contentAttribute, "Can I mention another story in a conversation?");
		questionExpression.setAttributeValue(belongsToAttribute, newConversation.getOid());
		services.save(questionExpression);

		Query query = new Query(questionExpression.getOid().getMomentless());
		IAttributeDefinition subsAttributeDefinition = questionExpression.getAssetType().getAttributeDefinition("Content");
		query.getSelection().add(subsAttributeDefinition);
		QueryResult result = services.retrieve(query);

		assertEquals("Can I mention another story in a conversation?", result.getAssets()[0].getAttribute(contentAttribute).getValue().toString());

		Asset answerExpression = services.createNew(expressionType, questionExpression.getOid());
		answerExpression.setAttributeValue(authorAttribute, services.getOid("Member:20"));
		answerExpression.setAttributeValue(authoredAtAttribute, DateUtils.addMinutes(DateTime.now().getValue(), 15));
		answerExpression.setAttributeValue(contentAttribute, "Yes I can!");
		answerExpression.setAttributeValue(inReplyToAttribute, questionExpression.getOid());
		services.save(answerExpression);

		newStory.addAttributeValue(mentionedInExpressionsAttribute, questionExpression.getOid());
		services.save(newStory);
		storyTobeMentioned.addAttributeValue(mentionedInExpressionsAttribute, answerExpression.getOid());
		services.save(storyTobeMentioned);

		query = new Query(answerExpression.getOid().getMomentless());
		subsAttributeDefinition = answerExpression.getAssetType().getAttributeDefinition("Content");
		query.getSelection().add(subsAttributeDefinition);
		result = services.retrieve(query);

		assertEquals("Yes I can!", result.getAssets()[0].getAttribute(contentAttribute).getValue().toString());
		assertEquals(1, result.getTotalAvaliable());
	}

	// @Test
	public void CreateStoryWithNestedTaskTest() throws V1Exception {

		IAssetType storyType = services.getMeta().getAssetType("Story");
		Asset newStory = services.createNew(storyType, _testProjectId);
		IAttributeDefinition nameAttribute = storyType.getAttributeDefinition("Name");
		IAttributeDefinition childrenAttribute = storyType.getAttributeDefinition("Children");
		String name = "Test Story" + _testProjectId + " Create story with nested task";
		newStory.setAttributeValue(nameAttribute, name);
		services.save(newStory);

		IAssetType taskType = services.getMeta().getAssetType("Task");
		Asset newTask = services.createNew(taskType, newStory.getOid());
		newTask.setAttributeValue(nameAttribute, "Test Task Nested in " + newStory.getOid());

		services.save(newTask);

		assertNotNull(newStory.getOid());
		assertNotNull(newTask.getOid());

		Query query = new Query(newStory.getOid().getMomentless());
		query.getSelection().add(childrenAttribute);
		Asset story = services.retrieve(query).getAssets()[0];

		assertEquals(1, story.getAttributes().size());
	}

	// @Test
	public void createStoryWithNestedTest() throws V1Exception {

		IAssetType storyType = services.getMeta().getAssetType("Story");
		Asset newStory = services.createNew(storyType, _testProjectId);
		IAttributeDefinition nameAttribute = storyType.getAttributeDefinition("Name");
		IAttributeDefinition childrenAttribute = storyType.getAttributeDefinition("Children");
		String name = "Test Story " + _testProjectId + " Create story with nested test";
		newStory.setAttributeValue(nameAttribute, name);
		services.save(newStory);

		IAssetType testType = services.getMeta().getAssetType("Test");
		Asset newTest = services.createNew(testType, newStory.getOid());
		newTest.setAttributeValue(nameAttribute, "Test Test Nested in " + newStory.getOid());

		services.save(newTest);

		assertNotNull(newStory.getOid());
		assertNotNull(newTest.getOid());

		Query query = new Query(newStory.getOid().getMomentless());
		query.getSelection().add(childrenAttribute);
		Asset story = services.retrieve(query).getAssets()[0];

		assertEquals(1, story.getAttributes().size());
	}

	
	//@Test
	 public void CreateStoryWithAttachmentTest() throws V1Exception, IOException {
	
	 String file = "/versionone.png";
	
	 assertNotNull("Test file missing", getClass().getResource(file));
	
	 String mimeType = MimeType.resolve(file);

	
	 IAttachments attachments = new Attachments(V1Connector
			 .withInstanceUrl(url)
			 .withUserAgentHeader("JavaSDKIntegrationTest", "1.0")
			 .withAccessToken(accessToken)
			 .useEndpoint("attachment.img/")
			 .build());

	 IAssetType storyType = services.getMeta().getAssetType("Story");
	 Asset newStory = services.createNew(storyType, _testProjectId);
	 IAttributeDefinition nameAttribute = storyType.getAttributeDefinition("Name");
	 IAttributeDefinition attachmentsAttribute = storyType.getAttributeDefinition("Attachments");
	 String name = "Test Story " + _testProjectId + "Create story with attachment";
	 newStory.setAttributeValue(nameAttribute, name);
	 services.save(newStory);
	
	 IAssetType attachmentType = services.getMeta().getAssetType("Attachment");
	 IAttributeDefinition attachmentAssetDef = attachmentType.getAttributeDefinition("Asset");
	 IAttributeDefinition attachmentContent = attachmentType.getAttributeDefinition("Content");
	 IAttributeDefinition attachmentContentType =
	 attachmentType.getAttributeDefinition("ContentType");
	 IAttributeDefinition attachmentFileName = attachmentType.getAttributeDefinition("Filename");
	 IAttributeDefinition attachmentName = attachmentType.getAttributeDefinition("Name");
	 Asset attachment = services.createNew(attachmentType, Oid.Null);
	 attachment.setAttributeValue(attachmentName, "Test Attachment on " + newStory.getOid());
	 attachment.setAttributeValue(attachmentFileName, file);
	 attachment.setAttributeValue(attachmentContentType, mimeType);
	 attachment.setAttributeValue(attachmentContent, "");
	 attachment.setAttributeValue(attachmentAssetDef, newStory.getOid());
	 services.save(attachment);
//	
	 String key = attachment.getOid().getKey().toString();
//	 //Stream input = new FileStream(file, FileMode.Open, FileAccess.Read)
	 InputStream inStream  = getClass().getResourceAsStream(file);
	 OutputStream output =  attachments.getWriter(key, mimeType);
//	 
	 byte[] buffer = new byte[inStream.available() + 1];
	 while (true){
		 int read = inStream.read(buffer, 0, buffer.length);
		 if (read <= 0)
			 break;
	
		 output.write(buffer, 0, read);
	 	}
	 
	 attachments.setWriter(key);
//	
	 Query query = new Query(newStory.getOid().getMomentless());
	 query.getSelection().add(attachmentsAttribute);
	 Asset story = services.retrieve(query).getAssets()[0];
	
	 assertEquals(1, story.getAttribute(attachmentsAttribute).getValues().length);
	 }

	 
	 //
	// [TestMethod]
	// [DeploymentItem("versionone.png")]
	// public void CreateStoryWithEmbeddedImage()
	// {
	// var services = GetServices();
	// string file = "versionone.png";
	//
	// Assert.IsTrue(File.Exists(file));
	//
	// string mimeType = MimeType.Resolve(file);
	// IAttachments attachments = new Attachments(V1Connector
	// .WithInstanceUrl(_v1InstanceUrl)
	// .WithUserAgentHeader(".NET_SDK_Integration_Test", "1.0")
	// .WithAccessToken(_v1AccessToken).UseEndpoint("embedded.img/")
	// .Build());
	//
	// var contextId = IntegrationTestsHelper.TestProjectOid;
	// var storyType = services.Meta.GetAssetType("Story");
	// var newStory = services.New(storyType, contextId);
	// var nameAttribute = storyType.GetAttributeDefinition("Name");
	// var descriptionAttribute = storyType.GetAttributeDefinition("Description");
	// var name = string.Format("Test Story {0} Create story with embedded image", contextId);
	// newStory.SetAttributeValue(nameAttribute, name);
	// newStory.SetAttributeValue(descriptionAttribute, "Test description");
	// services.Save(newStory);
	//
	// var embeddedImageType = services.Meta.GetAssetType("EmbeddedImage");
	// var newEmbeddedImage = services.New(embeddedImageType, Oid.Null);
	// var assetAttribute = embeddedImageType.GetAttributeDefinition("Asset");
	// var contentAttribute = embeddedImageType.GetAttributeDefinition("Content");
	// var contentTypeAttribute = embeddedImageType.GetAttributeDefinition("ContentType");
	// newEmbeddedImage.SetAttributeValue(assetAttribute, newStory.Oid);
	// newEmbeddedImage.SetAttributeValue(contentTypeAttribute, mimeType);
	// newEmbeddedImage.SetAttributeValue(contentAttribute, string.Empty);
	// services.Save(newEmbeddedImage);
	//
	// string key = newEmbeddedImage.Oid.Key.ToString();
	// using (Stream input = new FileStream(file, FileMode.Open, FileAccess.Read))
	// {
	// using (Stream output = attachments.GetWriteStream(key))
	// {
	// byte[] buffer = new byte[input.Length + 1];
	// while (true)
	// {
	// int read = input.Read(buffer, 0, buffer.Length);
	// if (read <= 0)
	// break;
	//
	// output.Write(buffer, 0, read);
	// }
	// }
	// }
	// attachments.SetWriteStream(key, mimeType);
	// newStory.SetAttributeValue(descriptionAttribute,
	// string.Format("<img src=\"{0}\" alt=\"\" data-oid=\"{1}\" />", "embedded.img/" + key,
	// newEmbeddedImage.Oid.Momentless));
	// services.Save(newStory);
	// }
	//

	//@Test
	public void createDefectTest() throws V1Exception {

		IAssetType defectType = services.getMeta().getAssetType("Defect");
		Asset newDefect = services.createNew(defectType, _testProjectId);
		IAttributeDefinition nameAttribute = defectType.getAttributeDefinition("Name");
		String name = "Test Defect " + _testProjectId + " Create defect";
		newDefect.setAttributeValue(nameAttribute, name);
		services.save(newDefect);

		assertNotNull(newDefect.getOid());
	}

	//@Test
	public void createDefectWithNestedTaskTest() throws V1Exception {

		IAssetType defectType = services.getMeta().getAssetType("Defect");
		Asset newDefect = services.createNew(defectType, _testProjectId);
		IAttributeDefinition nameAttribute = defectType.getAttributeDefinition("Name");
		IAttributeDefinition childrenAttribute = defectType.getAttributeDefinition("Children");
		String name = "Test Defect " + _testProjectId + " Create defect with nested task";
		newDefect.setAttributeValue(nameAttribute, name);
		services.save(newDefect);

		IAssetType taskType = services.getMeta().getAssetType("Task");
		Asset newTask = services.createNew(taskType, newDefect.getOid());
		newTask.setAttributeValue(nameAttribute, "Test Task Nested in " + newDefect.getOid());

		services.save(newTask);

		assertNotNull(newDefect.getOid());
		assertNotNull(newTask.getOid());

		Query query = new Query(newDefect.getOid().getMomentless());
		query.getSelection().add(childrenAttribute);
		Asset story = services.retrieve(query).getAssets()[0];

		assertEquals(1, story.getAttribute(childrenAttribute).getValues().length);
	}

	//@Test
	public void createDefectWithNestedTest() throws V1Exception {

		IAssetType defectType = services.getMeta().getAssetType("Defect");
		Asset newDefect = services.createNew(defectType, _testProjectId);
		IAttributeDefinition nameAttribute = defectType.getAttributeDefinition("Name");
		IAttributeDefinition childrenAttribute = defectType.getAttributeDefinition("Children");
		String name = "Test Defect " + _testProjectId + " Create defect with nested test";
		newDefect.setAttributeValue(nameAttribute, name);
		services.save(newDefect);

		IAssetType testType = services.getMeta().getAssetType("Test");
		Asset newTest = services.createNew(testType, newDefect.getOid());
		newTest.setAttributeValue(nameAttribute, "Test Test Nested in " + newDefect.getOid());
		services.save(newTest);

		assertNotNull(newDefect.getOid());
		assertNotNull(newTest.getOid());

		Query query = new Query(newDefect.getOid().getMomentless());
		query.getSelection().add(childrenAttribute);
		Asset story = services.retrieve(query).getAssets()[0];

		assertEquals(1, story.getAttribute(childrenAttribute).getValues().length);
	}

	//
	// [TestMethod]
	// [DeploymentItem("versionone.png")]
	// public void CreateDefectWithAttachment()
	// {
	// var services = GetServices();
	// string file = "versionone.png";
	//
	// string mimeType = MimeType.Resolve(file);
	// IAttachments attachments = new Attachments(V1Connector
	// .WithInstanceUrl(_v1InstanceUrl)
	// .WithUserAgentHeader(".NET_SDK_Integration_Test", "1.0")
	// .WithAccessToken(_v1AccessToken).UseEndpoint("attachment.img/")
	// .Build());
	//
	// var contextId = IntegrationTestsHelper.TestProjectOid;
	// var defectType = services.Meta.GetAssetType("Defect");
	// var newDefect = services.New(defectType, contextId);
	// var nameAttribute = defectType.GetAttributeDefinition("Name");
	// var attachmentsAttribute = defectType.GetAttributeDefinition("Attachments");
	// var name = string.Format("Test Defect {0} Create defect with attachment", contextId);
	// newDefect.SetAttributeValue(nameAttribute, name);
	// services.Save(newDefect);
	//
	// IAssetType attachmentType = services.Meta.GetAssetType("Attachment");
	// IAttributeDefinition attachmentAssetDef = attachmentType.GetAttributeDefinition("Asset");
	// IAttributeDefinition attachmentContent = attachmentType.GetAttributeDefinition("Content");
	// IAttributeDefinition attachmentContentType =
	// attachmentType.GetAttributeDefinition("ContentType");
	// IAttributeDefinition attachmentFileName = attachmentType.GetAttributeDefinition("Filename");
	// IAttributeDefinition attachmentName = attachmentType.GetAttributeDefinition("Name");
	// Asset attachment = services.New(attachmentType, Oid.Null);
	// attachment.SetAttributeValue(attachmentName, "Test Attachment on " + newDefect.Oid);
	// attachment.SetAttributeValue(attachmentFileName, file);
	// attachment.SetAttributeValue(attachmentContentType, mimeType);
	// attachment.SetAttributeValue(attachmentContent, string.Empty);
	// attachment.SetAttributeValue(attachmentAssetDef, newDefect.Oid);
	// services.Save(attachment);
	// string key = attachment.Oid.Key.ToString();
	// using (Stream input = new FileStream(file, FileMode.Open, FileAccess.Read))
	// {
	// using (Stream output = attachments.GetWriteStream(key))
	// {
	// byte[] buffer = new byte[input.Length + 1];
	// while (true)
	// {
	// int read = input.Read(buffer, 0, buffer.Length);
	// if (read <= 0)
	// break;
	//
	// output.Write(buffer, 0, read);
	// }
	// }
	// }
	// attachments.SetWriteStream(key, mimeType);
	//
	// var query = new Query(newDefect.Oid.Momentless);
	// query.Selection.Add(attachmentsAttribute);
	// var story = services.Retrieve(query).Assets[0];
	//
	// Assert.AreEqual(1, story.GetAttribute(attachmentsAttribute).Values.Cast<object>().Count());
	// }
	//
	// [TestMethod]
	// [DeploymentItem("versionone.png")]
	// public void CreateDefectWithEmbeddedImage()
	// {
	// var services = GetServices();
	// string file = "versionone.png";
	//
	// string mimeType = MimeType.Resolve(file);
	// IAttachments attachments = new Attachments(V1Connector
	// .WithInstanceUrl(_v1InstanceUrl)
	// .WithUserAgentHeader(".NET_SDK_Integration_Test", "1.0")
	// .WithAccessToken(_v1AccessToken).UseEndpoint("embedded.img/")
	// .Build());
	//
	// var contextId = IntegrationTestsHelper.TestProjectOid;
	// var defectType = services.Meta.GetAssetType("Defect");
	// var newDefect = services.New(defectType, contextId);
	// var nameAttribute = defectType.GetAttributeDefinition("Name");
	// var descriptionAttribute = defectType.GetAttributeDefinition("Description");
	// var name = string.Format("Test Defect {0} Create defect with embedded image", contextId);
	// newDefect.SetAttributeValue(nameAttribute, name);
	// services.Save(newDefect);
	//
	// var embeddedImageType = services.Meta.GetAssetType("EmbeddedImage");
	// var newEmbeddedImage = services.New(embeddedImageType, Oid.Null);
	// var assetAttribute = embeddedImageType.GetAttributeDefinition("Asset");
	// var contentAttribute = embeddedImageType.GetAttributeDefinition("Content");
	// var contentTypeAttribute = embeddedImageType.GetAttributeDefinition("ContentType");
	// newEmbeddedImage.SetAttributeValue(assetAttribute, newDefect.Oid);
	// newEmbeddedImage.SetAttributeValue(contentTypeAttribute, mimeType);
	// newEmbeddedImage.SetAttributeValue(contentAttribute, string.Empty);
	// services.Save(newEmbeddedImage);
	//
	// string key = newEmbeddedImage.Oid.Key.ToString();
	// using (Stream input = new FileStream(file, FileMode.Open, FileAccess.Read))
	// {
	// using (Stream output = attachments.GetWriteStream(key))
	// {
	// byte[] buffer = new byte[input.Length + 1];
	// while (true)
	// {
	// int read = input.Read(buffer, 0, buffer.Length);
	// if (read <= 0)
	// break;
	//
	// output.Write(buffer, 0, read);
	// }
	// }
	// }
	// attachments.SetWriteStream(key, mimeType);
	// newDefect.SetAttributeValue(descriptionAttribute,
	// string.Format("<img src=\"{0}\" alt=\"\" data-oid=\"{1}\" />", "embedded.img/" + key,
	// newEmbeddedImage.Oid.Momentless));
	// services.Save(newDefect);
	// }
	//
	//@Test
	public void createRequestTest() throws V1Exception {

		IAssetType requestType = services.getMeta().getAssetType("Request");
		 Asset newRequest = services.createNew(requestType, _testProjectId);
		IAttributeDefinition nameAttribute = requestType.getAttributeDefinition("Name");
		String name = "Test Request "+ _testProjectId + " Create request";
		newRequest.setAttributeValue(nameAttribute, name);
		services.save(newRequest);

		assertNotNull(newRequest.getOid());
	}

	//@Test
	public void createIssueTest() throws V1Exception {
	
		IAssetType issueType = services.getMeta().getAssetType("Issue");
		Asset newIssue = services.createNew(issueType, _testProjectId);
		IAttributeDefinition nameAttribute = issueType.getAttributeDefinition("Name");
		String name = "Test Issue " + _testProjectId + " Create issue";
		newIssue.setAttributeValue(nameAttribute, name);
		services.save(newIssue);

		assertNotNull(newIssue.getOid());
	}

	//@Test(expected = MetaException.class)
	public void CreateUnknownSingleAsset() {

		IAssetType unknownAsset = services.getMeta().getAssetType("Unknown");
	}

}