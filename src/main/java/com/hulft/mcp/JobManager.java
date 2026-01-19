package com.hulft.mcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages asynchronous job processing and status tracking.
 */
public class JobManager {
    private final Map<String, JobStatus> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public static class JobStatus {
        public String status; // "processing", "completed", "failed"
        public Map<String, Object> result;
        public String error;
    }
    
    public String createJob() {
        String jobId = java.util.UUID.randomUUID().toString();
        JobStatus status = new JobStatus();
        status.status = "processing";
        jobs.put(jobId, status);
        return jobId;
    }
    
    public void submitJob(String jobId, Runnable task) {
        JobStatus status = jobs.get(jobId);
        if (status == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }
        
        executor.submit(() -> {
            try {
                task.run();
            } catch (Exception e) { // NOPMD - Catch all for async error handling
                status.error = e.getMessage();
                status.status = "failed";
            }
        });
    }
    
    public JobStatus getJobStatus(String jobId) {
        return jobs.get(jobId);
    }
    
    public void completeJob(String jobId, Map<String, Object> result) {
        JobStatus status = jobs.get(jobId);
        if (status != null) {
            status.result = result;
            status.status = "completed";
        }
    }
    
    public void failJob(String jobId, String error) {
        JobStatus status = jobs.get(jobId);
        if (status != null) {
            status.error = error;
            status.status = "failed";
        }
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}
