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
    public void setId(final String id) { this._id=id; }

    //Task name
    private String name;
    public String getName() { return name; }
    public void setName(final String name) { this.name=name; }

    private String scheduleStart;
    public String getScheduleStart() { return scheduleStart; }
    public void setScheduleStart(final String scheduleStart) { this.scheduleStart=scheduleStart; }

    private String scheduleStop;
    public String getScheduleStop() { return scheduleStop; }
    public void setScheduleStop(final String scheduleStop) { this.scheduleStop=scheduleStop; }

    private String type;
    public String getType() { return type; }
    public void setType(final String type) { this.type=type; }


    public JobDescriptor getStartJobDescriptor(){
        return  toJobDescriptor("start-"+getName(),getScheduleStart(),"Start","In progress");
    }

    public JobDescriptor getStopJobDescriptor(){
        return  toJobDescriptor("stop-"+getName(),getScheduleStop(),"New","New");
    }

    public JobDescriptor toJobDescriptor(String name, String cron, String action, String status){
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
