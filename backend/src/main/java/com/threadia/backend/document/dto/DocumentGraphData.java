package com.threadia.backend.document.dto;
import java.util.List;
public record DocumentGraphData(List<GraphNode> nodes, List<GraphEdge> edges) { public record GraphNode(Long id, String type, String title, String issueKey, String status) { } public record GraphEdge(Long linkId, Long sourceDocumentId, Long targetIssueId) { } }
