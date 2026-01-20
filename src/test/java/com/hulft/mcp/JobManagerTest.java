package com.hulft.mcp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class JobManagerTest {
    
    private JobManager jobManager;
    
    @Before
    public void setUp() {
        jobManager = new JobManager();
    }
    
    @After
    public void tearDown() {
        jobManager.shutdown();
    }
    
    @Test
    public void testCreateJob() {
        final String jobId = jobManager.createJob();
        assertNotNull("Job ID should not be null", jobId);
        assertTrue("Job ID should be a UUID", jobId.length() > 0);
    }
    
    @Test
    public void testGetJobStatus() {
        final String jobId = jobManager.createJob();
        final JobManager.JobStatus status = jobManager.getJobStatus(jobId);
        assertNotNull("Status should not be null", status);
        assertEquals("Status should be processing", "processing", status.status);
    }
    
    @Test
    public void testCompleteJob() {
        final String jobId = jobManager.createJob();
        final java.util.Map<String, Object> result = java.util.Map.of("text", "test result");
        jobManager.completeJob(jobId, result);
        
        final JobManager.JobStatus status = jobManager.getJobStatus(jobId);
        assertEquals("Status should be completed", "completed", status.status);
        assertEquals("Result should match", result, status.result);
    }
    
    @Test
    public void testFailJob() {
        final String jobId = jobManager.createJob();
        jobManager.failJob(jobId, "Test error");
        
        final JobManager.JobStatus status = jobManager.getJobStatus(jobId);
        assertEquals("Status should be failed", "failed", status.status);
        assertEquals("Error should match", "Test error", status.error);
    }
    
    @Test
    public void testSubmitJob() throws Exception {
        final String jobId = jobManager.createJob();
        final boolean[] executed = {false};
        
        jobManager.submitJob(jobId, () -> executed[0] = true);
        
        Thread.sleep(100); // Wait for async execution
        assertTrue("Job should have executed", executed[0]);
    }
    
    @Test
    public void testSubmitJobWithException() throws Exception {
        final String jobId = jobManager.createJob();
        
        jobManager.submitJob(jobId, () -> {
            throw new RuntimeException("Test exception");
        });
        
        Thread.sleep(100); // Wait for async execution
        final JobManager.JobStatus status = jobManager.getJobStatus(jobId);
        assertEquals("Status should be failed", "failed", status.status);
    }
    
    @Test
    public void testGetNonExistentJob() {
        final JobManager.JobStatus status = jobManager.getJobStatus("nonexistent");
        assertNull("Status should be null for nonexistent job", status);
    }
}
