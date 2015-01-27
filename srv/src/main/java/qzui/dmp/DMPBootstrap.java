package qzui.dmp;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qzui.domain.JobDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by eshalagi on 1/22/15.
 */
public class DMPBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(DMPBootstrap.class);

    public static String SchedulerGroup = "dmptasks";
    public static String DMPUrl = "http://127.0.0.1";
    public static String URLApiTasks = DMPUrl + "/api/tasks";
    public static String QuartzUrl = DMPUrl + ":8084";//+Optional.fromNullable(System.getenv("PORT")).or("8080");

    /*
    * Get Tasks from migration platform
    * */
    public static ArrayList<TaskDefinition> getTasks() throws IOException, InterruptedException {
        String dmpTasksResponse = getUrl(URLApiTasks);

        int iteration = 30;
        while (dmpTasksResponse == null || dmpTasksResponse.isEmpty() || !dmpTasksResponse.contains("hits")) {
            Thread.sleep(300);
            dmpTasksResponse = getUrl(URLApiTasks);

            if (iteration-- == 0) {
                logger.error("The DMP server is unavailable {}", URLApiTasks);
                System.exit(1);
            }
        }

        logger.debug("Tasks response: {}", dmpTasksResponse);
        JSONObject jsonResponse = new JSONObject(dmpTasksResponse);
        if (jsonResponse.getJSONObject("hits").getInt("total") > 0) {
            Gson gson = new Gson();

            ArrayList<TaskDefinition> tasks = new ArrayList<>();
            JSONArray hitsArray = jsonResponse.getJSONObject("hits").getJSONArray("hits");
            for (int index = 0; index < hitsArray.length(); ++index) {
                try {
                    TaskDefinition task = gson.fromJson(hitsArray.getJSONObject(index).getJSONObject("_source").toString(), TaskDefinition.class);
                    task.setId(hitsArray.getJSONObject(index).getString("_id"));
                    tasks.add(task);
                } catch (Exception exc) {
                    logger.error("Error while creating JobTrigger from task {}", hitsArray.getJSONObject(index).toString());
                }
            }
            return tasks;
        }

        return null;
    }

    private static String getUrl(String uri) throws IOException {
        HttpGet req = new HttpGet(uri);
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(req)) {
            InputStream inputStream = response.getEntity().getContent();
            return IOUtils.toString(inputStream);
        }
    }

    private static void addJobDescriptor(final Scheduler scheduler, JobDescriptor jobDescriptor) {
        try {
            jobDescriptor.setGroup(SchedulerGroup);
            Set<Trigger> triggers = jobDescriptor.buildTriggers();
            JobDetail jobDetail = jobDescriptor.buildJobDetail();
            if (triggers.isEmpty()) {
                scheduler.addJob(jobDetail, false);
            } else {
                scheduler.scheduleJob(jobDetail, triggers, false);
            }
        } catch (SchedulerException e) {
            logger.error("Error: {}\n{}", e.getMessage(), e.getStackTrace());
        }
    }

    public static List<JobDescriptor> addNewJob(final Scheduler scheduler, TaskDefinition task) {
        List<JobDescriptor> descriptors = new ArrayList<>();
        JobDescriptor descriptor = null;
        if (task.getScheduleStart() != null && !task.getScheduleStart().isEmpty()) {
            descriptor = task.toStartJobDescriptor();
            descriptors.add(descriptor);
            addJobDescriptor(scheduler, descriptor);

            descriptor = task.toSendStartEventJobDescriptor();
            descriptors.add(descriptor);
            addJobDescriptor(scheduler, descriptor);
        }
        if (task.getScheduleStop() != null && !task.getScheduleStop().isEmpty()) {
            descriptor = task.toStopJobDescriptor();
            descriptors.add(descriptor);
            addJobDescriptor(scheduler, descriptor);

            descriptor = task.toSendStopEventJobDescriptor();
            descriptors.add(descriptor);
            addJobDescriptor(scheduler, descriptor);
        }
        return descriptors;
    }

    public static void deleteJob(final Scheduler scheduler, TaskDefinition task) {
        try {
            scheduler.deleteJob(new JobKey("start-" + task.getId(), SchedulerGroup));
            scheduler.deleteJob(new JobKey("stop-" + task.getId(), SchedulerGroup));
            scheduler.deleteJob(new JobKey("startevent-" + task.getId(), SchedulerGroup));
            scheduler.deleteJob(new JobKey("stopevent-" + task.getId(), SchedulerGroup));
        } catch (SchedulerException e) {
            logger.error("Error: {}\n{}", e.getMessage(), e.getStackTrace());
        } catch (Exception e) {
            logger.error("Error: {}\n{}", e.getMessage(), e.getStackTrace());
        }
    }

    public static void subscribeToDMPEvents() {
        try {
            HttpPost request = new HttpPost(DMPUrl + "/api/callbacks/bulk");
            StringEntity params = new StringEntity("{\"_id\":\"quartz-task-post\", \"pattern\": \"/api/tasks\", \"method\": \"POST\", \"url\":\"" + QuartzUrl + "/api/posttask\"}\n" +
                    "{\"_id\":\"quartz-task-post-id\", \"pattern\": \"/api/tasks/:id\", \"method\": \"POST\", \"url\":\"" + QuartzUrl + "/api/posttask\"}\n" +
                    "{\"_id\":\"quartz-task-patch-id\", \"pattern\": \"/api/tasks/:id\", \"method\": \"PATCH\", \"url\":\"" + QuartzUrl + "/api/updatetask\"}\n" +
                    "{\"_id\":\"quartz-task-delete\", \"pattern\": \"/api/tasks\", \"method\": \"DELETE\", \"url\":\"" + QuartzUrl + "/api/deletetask\"}\n" +
                    "{\"_id\":\"quartz-task-delete-id\", \"pattern\": \"/api/tasks/:id\", \"method\": \"DELETE\", \"url\":\"" + QuartzUrl + "/api/deletetask\"}");
            request.addHeader("content-type", "application/json");
            request.setEntity(params);
            CloseableHttpClient client = HttpClients.createDefault();
            HttpResponse result = client.execute(request);
            String json = EntityUtils.toString(result.getEntity(), "UTF-8");
            logger.info("POST {} => {}\n{}", DMPUrl + "/api/callbacks/bulk", result.getStatusLine(), json);
        } catch (IOException e) {
            logger.error("Error: {}\n{}", e.getMessage(), e.getStackTrace());
        }
    }

    public static void initializeJobsFromTasks(final Scheduler scheduler) {
        try {
            ArrayList<TaskDefinition> tasks = getTasks();
            if (tasks != null)
                for (TaskDefinition task : tasks) {
                    deleteJob(scheduler, task);
                    addNewJob(scheduler, task);
                }
        } catch (IOException e) {
            logger.error("Error: {}\n{}", e.getMessage(), e.getStackTrace());
        } catch (Exception e) {
            logger.error("Error: {}\n{}", e.getMessage(), e.getStackTrace());
        }
    }
}
