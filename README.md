# WorkQueue

A Distributed Background Task Processing System written in Java, using Redis for job queuing.

**High level overview**
    ![overview](WorkQueue.png)


## What's the need for this?

This system is designed to handle the processing and execution of background tasks concurrently to improve user experience.

**Example:** When a user signs in to your website and clicks the login button, you might want to send them a welcome email. If that email task is part of the API call, the user would have to wait until the email is sent. Instead, you can add the "send_email" task to WorkQueue and let it handle the execution in the background.

**Note:** This is built to be modular — any type of job can be added to it, not just sending emails. You just need to add the logic for that job as described below.

## How to Run

### Prerequisites
*   Java 17 or higher
*   Apache Maven
*   Redis server running on `localhost:6379`

### Running the Services

1.  **Start the Producer:**
    Open a terminal and run:
    ```bash
    cd producer
    mvn spring-boot:run
    ```
    The producer will start on port `8080`.

2.  **Start the Worker:**
    Open another terminal and run:
    ```bash
    cd worker
    mvn spring-boot:run
    ```
    The worker will start on port `8081`.

## Services

This project provides two independent services:

### 1. Producer

Provides a `/enqueue` route to add your jobs/tasks.

#### How to add a job?

- Send an HTTP POST request to the `/enqueue` route: `http://localhost:8080/enqueue`
- It accepts a task in this format (JSON):

**Example: An inbuilt task the system supports is sending an email. Its JSON request would look like this:**

```json
{
    "type": "send_email",
    "retries": 3,
    "payload": {
        "to": "worldisweird2020@gmail.com",
        "subject": "testing producer"
    }
}
```

You can use `curl` to send the request:
```bash
curl -X POST -H "Content-Type: application/json" -d '{
    "type": "send_email",
    "retries": 3,
    "payload": {
        "to": "worldisweird2020@gmail.com",
        "subject": "testing producer"
    }
}' http://localhost:8080/enqueue
```

- **type** - REQUIRED. Tells the producer the type of job being added to the queue.
- **retries** - Number of times the system should try to enqueue the job if it fails.
- **payload** - Contains details about the task in key-value pairs.

This is the Java class it accepts:

```java
public class Task {
    private String type;
    private Map<String, Object> payload;
    private int retries;
    // getters and setters
}
```

The response will look like this:
`Task enqueued successfully: send_email`

### 2. Worker

- Takes the jobs from the queue in a reliable manner and executes them.
- Provides a `/metrics` endpoint to view statistics.

#### How to view the status of your job?

Send an HTTP GET request to `http://localhost:8081/metrics`.

Using `curl`:
```bash
curl http://localhost:8081/metrics
```

This will give a response like this:

```json
{
  "total_jobs_in_queue": 0,
  "jobs_done": 1,
  "jobs_failed": 0
}
```

- **total_jobs_in_queue** - Number of jobs inside the Redis queue at that moment.
- **jobs_done** - Total number of jobs executed so far.
- **jobs_failed** - Number of jobs that failed to execute, if any.

## How are jobs executed?

Inside `worker/src/main/java/com/workqueue/worker/WorkerService.java`, you will find the `processTask` method. The switch case makes it modular enough so you can add your job type just by adding another case.

**To add a new type of task:** Just add its function inside a new switch case, and that's it!

```java
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
```

## Additional features

- **Concurrency** is provided using Spring's `@Scheduled` annotation, which executes tasks in a background thread pool.
- **Logging** of each event is printed to the standard output.

---

Created by - mayankdevelops
