package com.metric.analyst.agent.service.query;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 内存向量存储 - 简化版
 */
@Service
public class InMemoryVectorStore {

    private final Map<String, String> documents = new HashMap<>();

    public void addDocument(String id, String text) {
        documents.put(id, text);
    }

    public List<SearchResult> search(String query, int topK) {
        List<SearchResult> results = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : documents.entrySet()) {
            double score = calculateScore(query, entry.getValue());
            if (score > 0) {
                results.add(new SearchResult(entry.getKey(), entry.getValue(), score));
            }
        }
        
        results.sort((a, b) -> Double.compare(b.score(), a.score()));
        return results.stream().limit(topK).toList();
    }

    private double calculateScore(String query, String text) {
        String q = query.toLowerCase();
        String t = text.toLowerCase();
        
        if (t.contains(q)) return 1.0;
        
        String[] words = q.split("");
        int matchCount = 0;
        for (String word : words) {
            if (word.trim().isEmpty()) continue;
            if (t.contains(word)) matchCount++;
        }
        
        return (double) matchCount / words.length;
    }

    public void clear() {
        documents.clear();
    }

    public record SearchResult(String id, String text, double score) {}
}
