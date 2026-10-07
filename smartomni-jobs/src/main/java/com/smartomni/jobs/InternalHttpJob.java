package com.smartomni.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Calls a Java service over the private Compose network; Quartz owns the schedule. */
@DisallowConcurrentExecution
public class InternalHttpJob implements Job {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String token = System.getenv("INTERNAL_JOB_TOKEN");
        if (token == null || token.length() < 32) {
            throw new JobExecutionException("INTERNAL_JOB_TOKEN must be at least 32 characters");
        }
        String url = context.getMergedJobDataMap().getString("url");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("X-Internal-Job-Token", token)
                    .timeout(Duration.ofMinutes(2))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> response = CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new JobExecutionException("Job endpoint returned HTTP " + response.statusCode());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new JobExecutionException("Job call interrupted", ex);
        } catch (Exception ex) {
            throw new JobExecutionException("Job call failed: " + url, ex);
        }
    }
}
