package com.hulft.mcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages asynchronous job processing and status tracking.
 * Provides a thread pool for background task execution with job status monitoring.
 */
public class JobManager {
    private final Map<String, JobStatus> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    /**
     * Represents the status of an asynchronous job.
     */
    public static class JobStatus {
        /** Current status: "processing", "completed", or "failed" */
        public String status;
        /** Result data when job completes successfully */
        public Map<String, Object> result;
        /** Error message if job fails */
        public String error;
    }
    
    /**
     * Creates a new job with "processing" status.
     *
     * @return unique job identifier
     */
    public String createJob() {
        final String jobId = java.util.UUID.randomUUID().toString();
        final JobStatus status = new JobStatus();
        status.status = "processing";
        jobs.put(jobId, status);
        return jobId;
    }
    
    /**
     * Submits a task for asynchronous execution.
     *
     * @param jobId the job identifier
     * @param task the task to execute
     * @throws IllegalArgumentException if job ID not found
     */
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
    
    /**
     * Retrieves the current status of a job.
     *
     * @param jobId the job identifier
     * @return job status, or null if not found
     */
    public JobStatus getJobStatus(final String jobId) {
        return jobs.get(jobId);
    }
    
    /**
     * Marks a job as completed with result data.
     *
     * @param jobId the job identifier
     * @param result the result data
     */
    public void completeJob(final String jobId, final Map<String, Object> result) {
        final JobStatus status = jobs.get(jobId);
        if (status != null) {
            status.result = result;
            status.status = "completed";
        }
    }
    
    /**
     * Marks a job as failed with an error message.
     *
     * @param jobId the job identifier
     * @param error the error message
     */
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
