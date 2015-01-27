package qzui.dmp;

import qzui.domain.HttpJobDefinition;
import qzui.domain.JobDescriptor;
import qzui.domain.TriggerDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by eshalagi on 1/22/15.
 */
public class TaskDefinition {
    //Document id in Elasticsearch index
    private String _id;
    public String getId() { return _id; }
    public TaskDefinition setId(final String id) { this._id=id; return  this; }

    private String projectId;
    public String getProjectId() { return projectId; }
    public TaskDefinition setProjectId(final String projectId) { this.projectId=projectId; return  this; }

    //Task name
    private String name;
    public String getName() { return name; }
    public TaskDefinition setName(final String name) { this.name=name; return  this; }

    private String scheduleStart;
    public String getScheduleStart() { return scheduleStart; }
    public TaskDefinition setScheduleStart(final String scheduleStart) { this.scheduleStart=scheduleStart; return  this; }

    private String scheduleStop;
    public String getScheduleStop() { return scheduleStop; }
    public TaskDefinition setScheduleStop(final String scheduleStop) { this.scheduleStop=scheduleStop; return  this; }

    private String type;
    public String getType() { return type; }
    public TaskDefinition setType(final String type) { this.type=type; return  this; }


    public JobDescriptor toStartJobDescriptor(){
        return  toTaskJobDescriptor("start-"+getId(),getScheduleStart(),"Start","In Progress");
    }

    public JobDescriptor toStopJobDescriptor(){
        return  toTaskJobDescriptor("stop-"+getId(),getScheduleStop(),"New","New");
    }

    public JobDescriptor toSendStartEventJobDescriptor(){
        return  toEventJobDescriptor("startevent-"+getId(),getScheduleStart(),"A scheduled action update of task action to Start","action=Start,status=In progress");
    }

    public JobDescriptor toSendStopEventJobDescriptor(){
        return  toEventJobDescriptor("stopevent-"+getId(),getScheduleStop(),"A scheduled action update of task action to New","action=New,status=New");
    }

    public JobDescriptor toEventJobDescriptor(String name, String cron, String message, String details){
        HttpJobDefinition.HttpJobDescriptor jobDescriptor=new HttpJobDefinition.HttpJobDescriptor();
        jobDescriptor.setUrl(DMPBootstrap.DMPUrl+"/api/events");
        jobDescriptor.setMethod("POST");
        jobDescriptor.setBody("{\"message\": \""+message+"\", \"details\": \""+details+"\", \"severity\":\"Info\", \"severityId\":\"3\", \"projectId\":\""+getProjectId()+"\", \"task.name\": \""+getName()+"\"}");
        jobDescriptor.setName(name);
        List<TriggerDescriptor> triggerDescriptors = new ArrayList<>();
        TriggerDescriptor trDescriptor=new TriggerDescriptor();
        trDescriptor.setCron(cron);
        trDescriptor.setName(name);
        trDescriptor.setGroup(DMPBootstrap.SchedulerGroup);
        triggerDescriptors.add(trDescriptor);
        jobDescriptor.setTriggerDescriptors(triggerDescriptors);

        return  jobDescriptor;
    }

    public JobDescriptor toTaskJobDescriptor(String name, String cron, String action, String status){
        HttpJobDefinition.HttpJobDescriptor jobDescriptor=new HttpJobDefinition.HttpJobDescriptor();
        jobDescriptor.setUrl(DMPBootstrap.DMPUrl+"/api/tasks/"+getId());
        jobDescriptor.setMethod("PATCH");
        jobDescriptor.setBody("{\"action\": \""+action+"\", \"status\": \""+status+"\"}");
        jobDescriptor.setName(name);
        List<TriggerDescriptor> triggerDescriptors = new ArrayList<>();
        TriggerDescriptor trDescriptor=new TriggerDescriptor();
        trDescriptor.setCron(cron);
        trDescriptor.setName(name);
        trDescriptor.setGroup(DMPBootstrap.SchedulerGroup);
        triggerDescriptors.add(trDescriptor);
        jobDescriptor.setTriggerDescriptors(triggerDescriptors);

        return  jobDescriptor;
    }
}
