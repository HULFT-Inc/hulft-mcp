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
        final String jobId = java.util.UUID.randomUUID().toString();
        final JobStatus status = new JobStatus();
        status.status = "processing";
        jobs.put(jobId, status);
        return jobId;
    }
    
    public void submitJob(final String jobId, final Runnable task) {
        final JobStatus status = jobs.get(jobId);
        if (status == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }
        
        executor.submit(() -> {
            try {
                task.run();
            } catch (final Exception e) { // NOPMD - Catch all for async error handling
                status.error = e.getMessage();
                status.status = "failed";
            }
        });
    }
    
    public JobStatus getJobStatus(final String jobId) {
        return jobs.get(jobId);
    }
    
    public void completeJob(final String jobId, final Map<String, Object> result) {
        final JobStatus status = jobs.get(jobId);
        if (status != null) {
            status.result = result;
            status.status = "completed";
        }
    }
    
    public void failJob(final String jobId, final String error) {
        final JobStatus status = jobs.get(jobId);
        if (status != null) {
            status.error = error;
            status.status = "failed";
        }
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}
