package qzui.rest;

import com.google.common.base.Optional;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qzui.dmp.*;
import qzui.domain.JobDefinition;
import qzui.domain.JobDescriptor;
import restx.annotations.DELETE;
import restx.annotations.GET;
import restx.annotations.POST;
import restx.annotations.RestxResource;
import restx.factory.Component;
import restx.security.PermitAll;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Date: 18/2/14
 * Time: 21:31
 */
@RestxResource
@Component
public class JobResource {
    private static final Logger logger = LoggerFactory.getLogger(JobResource.class);
    private final Scheduler scheduler;
    private final Collection<JobDefinition> definitions;

    public JobResource(Scheduler scheduler, Collection<JobDefinition> definitions) {
        this.scheduler = scheduler;
        this.definitions = definitions;
    }

    @PermitAll
    @POST("/createtask")
    public List<JobDescriptor> createTask(CallbackResponseCreateTask response) {
        logger.debug("call createTask");
        return DMPBootstrap.addNewJob(scheduler, response.getTask());
    }

    @PermitAll
    @POST("/bulkcreatetask")
    public List<JobDescriptor> bulkCreateTask(CallbackResponseBulkCreateTask response) {
        logger.debug("call bulkCreateTask");
        return  null;
        //return DMPBootstrap.addNewJob(scheduler, response.getTasks());
    }

    @PermitAll
    @POST("/updatetask")
    public List<JobDescriptor> updateTask(CallbackResponseUpdateTask response) {
        logger.debug("call updateTask");
        DMPBootstrap.deleteJob(scheduler, response.getTask());
        return DMPBootstrap.addNewJob(scheduler, response.getTask());
    }

    @PermitAll
    @POST("/bulkupdatetask")
    public List<JobDescriptor> bulkUpdateTask(CallbackResponseBulkUpdateTask response) {
        logger.debug("call bulkUpdateTask");
        return  null;
        //return DMPBootstrap.addNewJob(scheduler, response.getTasks());
    }

    @PermitAll
    @POST("/deletetask")
    public Boolean deleteTask(CallbackResponseDeleteTask response) {
        logger.debug("call deleteTask");
        DMPBootstrap.deleteJob(scheduler, response.getTask());
        return true;
    }

    /*
    Example:

        {"type":"log", "name":"test2", "group":"log", "triggers": [{"cron":"0/2 * * * * ?"}]}

        {"type":"http", "name":"google-humans", "method":"GET", "url":"http://www.google.com/humans.txt", "triggers": [{"when":"now"}]}
     */
    @PermitAll
    @POST("/groups/{group}/jobs")
    public JobDescriptor addJob(String group, JobDescriptor jobDescriptor) {
        try {
            jobDescriptor.setGroup(group);
            Set<Trigger> triggers = jobDescriptor.buildTriggers();
            JobDetail jobDetail = jobDescriptor.buildJobDetail();
            if (triggers.isEmpty()) {
                scheduler.addJob(jobDetail, false);
            } else {
                scheduler.scheduleJob(jobDetail, triggers, false);
            }
            return jobDescriptor;
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    @PermitAll
    @GET("/jobs")
    public Set<JobKey> getJobKeys() {
        try {
            return scheduler.getJobKeys(GroupMatcher.anyJobGroup());
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    @PermitAll
    @GET("/groups/{group}/jobs")
    public Set<JobKey> getJobKeysByGroup(String group) {
        try {
            return scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group));
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    @PermitAll
    @GET("/groups/{group}/jobs/{name}")
    public Optional<JobDescriptor> getJob(String group, String name) {
        try {
            JobDetail jobDetail = scheduler.getJobDetail(new JobKey(name, group));

            if (jobDetail == null) {
                return Optional.absent();
            }

            for (JobDefinition definition : definitions) {
                if (definition.acceptJobClass(jobDetail.getJobClass())) {
                    return Optional.of(definition.buildDescriptor(
                            jobDetail, scheduler.getTriggersOfJob(jobDetail.getKey())));
                }
            }

            throw new IllegalStateException("can't find job definition for " + jobDetail
                    + " - available job definitions: " + definitions);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    @PermitAll
    @DELETE("/groups/{group}/jobs/{name}")
    public void deleteJob(String group, String name) {
        try {
            scheduler.deleteJob(new JobKey(name, group));
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
}
