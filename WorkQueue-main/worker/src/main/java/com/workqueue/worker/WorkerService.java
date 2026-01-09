package com.workqueue.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workqueue.common.Metrics;
import com.workqueue.common.Task;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WorkerService {

    private final Jedis jedis = new Jedis("localhost", 6379);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String QUEUE_NAME = "work_queue";
    private final AtomicInteger jobsDone = new AtomicInteger(0);
    private final AtomicInteger jobsFailed = new AtomicInteger(0);

    @Scheduled(fixedRate = 5000) // Poll every 5 seconds
    public void processQueue() {
        List<String> tasks = jedis.brpop(0, QUEUE_NAME);
        if (tasks != null && tasks.size() == 2) {
            String taskJson = tasks.get(1);
            try {
                Task task = objectMapper.readValue(taskJson, Task.class);
                processTask(task);
                jobsDone.incrementAndGet();
            } catch (JsonProcessingException e) {
                jobsFailed.incrementAndGet();
            }
        }
    }

    private void processTask(Task task) {
        System.out.println("Processing task: " + task.getType());
        switch (task.getType()) {
            case "send_email":
                System.out.println("Sending email to " + task.getPayload().get("to") + " with subject " + task.getPayload().get("subject"));
                break;
            case "resize_image":
                System.out.println("Resizing image to x coordinate: " + task.getPayload().get("new_x") + " y coordinate: " + task.getPayload().get("new_y"));
                break;
            case "generate_pdf":
                System.out.println("Generating pdf...");
                break;
            default:
                System.out.println("Unsupported task type: " + task.getType());
                jobsFailed.incrementAndGet();
        }
    }

    public Metrics getMetrics() {
        long totalJobsInQueue = jedis.llen(QUEUE_NAME);
        return new Metrics(totalJobsInQueue, jobsDone.get(), jobsFailed.get());
    }
} 