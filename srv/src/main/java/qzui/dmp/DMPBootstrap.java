package qzui.dmp;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qzui.domain.JobDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;

/**
 * Created by eshalagi on 1/22/15.
 */
public class DMPBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(DMPBootstrap.class);

    public static String SchedulerGroup="dmptasks";
    public static String DMPUrl="http://54.144.233.53";
    public static String QuartzUrl=DMPUrl+":"+Optional.fromNullable(System.getenv("PORT")).or("8080");
    private static Scheduler scheduler;

    public static ArrayList<TaskDefinition> getTasks() throws  IOException{
        String dmpTasksResponse=getUrl(DMPUrl+"/api/tasks");

        if(dmpTasksResponse!=null && !dmpTasksResponse.isEmpty()){
            JSONObject jsonResponse = new JSONObject(dmpTasksResponse);
            if(jsonResponse.getJSONObject("hits").getInt("total")>0){
                Gson gson = new Gson();

                ArrayList<TaskDefinition> tasks=new ArrayList<TaskDefinition>();
                JSONArray hitsArray = jsonResponse.getJSONObject("hits").getJSONArray("hits");
                for(int index=0;index<hitsArray.length();++index){
                    TaskDefinition task = gson.fromJson(hitsArray.getJSONObject(index).getJSONObject("_source").toString(), TaskDefinition.class);
                    task.setId(hitsArray.getJSONObject(index).getString("_id"));
                    tasks.add(task);
                }
                return tasks;
            }
        }
        return null;
    }

    private static String getUrl(String uri) throws IOException {
        HttpGet req = new HttpGet(uri);
        try ( CloseableHttpClient client = HttpClients.createDefault();
              CloseableHttpResponse response = client.execute(req) ) {
            InputStream inputStream = response.getEntity().getContent();
            return IOUtils.toString(inputStream);
        }
    }

    private static void addJobDescriptor(JobDescriptor jobDescriptor){
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
            throw new RuntimeException(e);
        }
    }

    public static void addNewJob(TaskDefinition task){
        if(task.getScheduleStart()!=null && !task.getScheduleStart().isEmpty()){
            addJobDescriptor(task.getStartJobDescriptor());
        }
        if(task.getScheduleStop()!=null && !task.getScheduleStop().isEmpty()){
            addJobDescriptor(task.getStopJobDescriptor());
        }
    }

    public static  void subscribeToDMPEvents(){
        try{
            HttpPost request = new HttpPost(DMPUrl+"/api/callbacks/bulk");
            StringEntity params = new StringEntity("{\"_id\":\"quartz-task-post\", \"pattern\": \"/api/tasks\", \"method\": \"POST\", \"url\":\""+QuartzUrl+"/posttask\"}\n"+
                    "{\"_id\":\"quartz-task-delete\", \"pattern\": \"/api/tasks\", \"method\": \"DELETE\", \"url\":\""+QuartzUrl+"/deletetask\"}");
            request.addHeader("content-type", "application/json");
            request.setEntity(params);
            CloseableHttpClient client = HttpClients.createDefault();
            HttpResponse result = client.execute(request);
            String json = EntityUtils.toString(result.getEntity(), "UTF-8");
            logger.info("POST {} => {}\n{}", DMPUrl+"/api/callbacks/bulk", result.getStatusLine(), json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void initializeJobsFromTasks(){
        try {
            ArrayList<TaskDefinition> tasks=getTasks();
            if(tasks!=null)
                for(TaskDefinition task : tasks){
                    addNewJob(task);
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static  void setScheduler(final Scheduler scheduler){
        DMPBootstrap.scheduler=scheduler;
    }
}
