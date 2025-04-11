package org.nc.springaitest.services;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EvaluationMetrics {

    private final Map<String, List<Double>> scoreDistributions = new ConcurrentHashMap<>();
    private final Map<String, Integer> bandCounts = new ConcurrentHashMap<>();

    public synchronized void trackEvaluation(String band, String criteria, double score) {
        String key = band + "-" + criteria;
        scoreDistributions.computeIfAbsent(key, k -> new ArrayList<>()).add(score);
        bandCounts.merge(band, 1, Integer::sum);
    }

    public Map<String, Double> getAverageScoresByBand() {
        Map<String, Double> averages = new HashMap<>();
        scoreDistributions.forEach((key, scores) -> {
            double avg = scores.stream().mapToDouble(d -> d).average().orElse(0);
            averages.put(key, avg);
        });
        return averages;
    }

    public Map<String, Integer> getBandDistribution() {
        return new HashMap<>(bandCounts);
    }

    public void resetMetrics() {
        scoreDistributions.clear();
        bandCounts.clear();
    }
}