package com.threadia.backend.document.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.threadia.backend.document.dto.DocumentData;
import com.threadia.backend.document.dto.DocumentGraphData;
import com.threadia.backend.document.dto.DocumentLinkData;
import com.threadia.backend.document.dto.DocumentLinkResponse;
import com.threadia.backend.document.dto.DocumentLinksData;
import com.threadia.backend.document.dto.DocumentResponse;
import com.threadia.backend.document.dto.DocumentsData;
import com.threadia.backend.document.dto.DeletedData;
import com.threadia.backend.document.service.DocumentService;
import com.threadia.backend.issue.dto.IssueResponse;
import com.threadia.backend.issue.dto.IssuesData;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(DocumentController.class)
class DocumentControllerTest {
	@Autowired private MockMvc mockMvc;
	@MockitoBean private DocumentService documentService;
	@Test
	void listPassesFiltersAndReturnsEnvelope() throws Exception {
		when(documentService.list("token", 20L, null, "api", false)).thenReturn(new DocumentsData(List.of(response())));
		mockMvc.perform(get("/api/v1/projects/20/documents").header(HttpHeaders.AUTHORIZATION, "Bearer token").param("q", "api"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.documents[0].title").value("API 문서")).andExpect(jsonPath("$.meta.requestId").exists());
	}
	@Test
	void createReturnsCreatedDocument() throws Exception {
		when(documentService.create(eq("token"), eq(20L), any())).thenReturn(new DocumentData(response()));
		mockMvc.perform(post("/api/v1/projects/20/documents").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"API 문서\",\"type\":\"DOC\",\"content\":\"# Markdown\"}"))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.data.document.type").value("DOC")).andExpect(jsonPath("$.meta.requestId").exists());
	}
	@Test
	void createRejectsBlankTitle() throws Exception {
		mockMvc.perform(post("/api/v1/projects/20/documents").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\" \",\"type\":\"DOC\",\"content\":\"x\"}"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}
	@Test
	void createTrimsBeforeApplyingMaximumTitleLength() throws Exception {
		String title = " ".repeat(20) + "a".repeat(200) + " ".repeat(20);
		when(documentService.create(eq("token"), eq(20L), any())).thenReturn(new DocumentData(response()));
		mockMvc.perform(post("/api/v1/projects/20/documents").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"" + title + "\",\"type\":\"DOC\",\"content\":\"# Markdown\"}"))
			.andExpect(status().isCreated());
	}
	@Test
	void createRejectsTitleWhoseTrimmedLengthExceedsMaximum() throws Exception {
		mockMvc.perform(post("/api/v1/projects/20/documents").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\" " + "a".repeat(201) + " \",\"type\":\"DOC\",\"content\":\"# Markdown\"}"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}
	@Test
	void updateRequiresTypeAndTrashUsesNoRequestBody() throws Exception {
		mockMvc.perform(patch("/api/v1/documents/1").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"문서\",\"content\":\"# Markdown\"}"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
		when(documentService.trash("token", 1L)).thenReturn(new DocumentData(response()));
		mockMvc.perform(post("/api/v1/documents/1/trash").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.document.createdAt").value("2026-07-15T00:00:00Z"));
	}
	@Test
	void detailReturnsDocumentEnvelope() throws Exception {
		when(documentService.detail("token", 1L)).thenReturn(new DocumentData(response()));
		mockMvc.perform(get("/api/v1/documents/1").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.document.id").value(1)).andExpect(jsonPath("$.data.document.content").value("# Markdown")).andExpect(jsonPath("$.meta.requestId").exists());
	}
	@Test
	void updateReturnsDocumentEnvelope() throws Exception {
		when(documentService.update(eq("token"), eq(1L), any())).thenReturn(new DocumentData(response()));
		mockMvc.perform(patch("/api/v1/documents/1").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"문서\",\"type\":\"DOC\",\"content\":\"# Markdown\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.document.title").value("API 문서")).andExpect(jsonPath("$.meta.requestId").exists());
	}
	@Test
	void restoreReturnsDocumentEnvelope() throws Exception {
		when(documentService.restore("token", 1L)).thenReturn(new DocumentData(response()));
		mockMvc.perform(post("/api/v1/documents/1/restore").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.document.trashedAt").value(nullValue())).andExpect(jsonPath("$.meta.requestId").exists());
	}
	@Test
	void graphReturnsNodesAndEdgesEnvelope() throws Exception {
		DocumentGraphData graph = new DocumentGraphData(List.of(new DocumentGraphData.GraphNode(1L, "DOCUMENT", "API 문서", null, null)), List.of(new DocumentGraphData.GraphEdge(10L, 1L, 2L)));
		when(documentService.graph("token", 20L)).thenReturn(graph);
		mockMvc.perform(get("/api/v1/projects/20/documents/graph").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.nodes[0].type").value("DOCUMENT")).andExpect(jsonPath("$.data.edges[0].linkId").value(10)).andExpect(jsonPath("$.meta.requestId").exists());
	}
	@Test
	void linkListReturnsLinksEnvelope() throws Exception {
		when(documentService.listLinks("token", 20L)).thenReturn(new DocumentLinksData(List.of(linkResponse())));
		mockMvc.perform(get("/api/v1/projects/20/document-issue-links").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.links[0].documentId").value(1)).andExpect(jsonPath("$.data.links[0].issueId").value(2)).andExpect(jsonPath("$.meta.requestId").exists());
	}
	@Test
	void createLinkReturnsCreatedLinkEnvelope() throws Exception {
		when(documentService.createLink(eq("token"), eq(20L), any())).thenReturn(new DocumentLinkData(linkResponse()));
		mockMvc.perform(post("/api/v1/projects/20/document-issue-links").header(HttpHeaders.AUTHORIZATION, "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"documentId\":1,\"issueId\":2}"))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.data.link.id").value(10)).andExpect(jsonPath("$.meta.requestId").exists());
	}
	@Test
	void deleteLinkReturnsDeletedEnvelope() throws Exception {
		when(documentService.deleteLink("token", 10L)).thenReturn(new DeletedData(true));
		mockMvc.perform(delete("/api/v1/document-issue-links/10").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.deleted").value(true)).andExpect(jsonPath("$.meta.requestId").exists());
	}
	@Test
	void documentsForIssueReturnsDocumentEnvelope() throws Exception {
		when(documentService.documentsForIssue("token", 2L)).thenReturn(new DocumentsData(List.of(response())));
		mockMvc.perform(get("/api/v1/issues/2/documents").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.documents[0].id").value(1)).andExpect(jsonPath("$.meta.requestId").exists());
	}
	@Test
	void issuesForDocumentReturnsIssueEnvelope() throws Exception {
		when(documentService.issuesForDocument("token", 1L)).thenReturn(new IssuesData(List.of(new IssueResponse(2L, "THD-1", "이슈", "TODO"))));
		mockMvc.perform(get("/api/v1/documents/1/issues").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.issues[0].issueKey").value("THD-1")).andExpect(jsonPath("$.meta.requestId").exists());
	}
	private static DocumentLinkResponse linkResponse() { return new DocumentLinkResponse(10L, 20L, 1L, 2L, 3L); }
	private static DocumentResponse response() { return new DocumentResponse(1L, 20L, 2L, "API 문서", "DOC", "# Markdown", null, null, "2026-07-15T00:00:00Z", "2026-07-15T00:00:00Z"); }
}
