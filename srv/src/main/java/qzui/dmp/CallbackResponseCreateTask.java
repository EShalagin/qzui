package qzui.dmp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import restx.factory.Component;

/**
 * Created by eshalagi on 1/22/15.
 */
public class CallbackResponseCreateTask extends   CallbackResponse {
    private String id;
    public String getId() { return id; }
    public CallbackResponse setId(final String id) { this.id=id; return  this; }


    @JsonProperty("data")
    private TaskDefinition task;
    public TaskDefinition getTask(){task.setId(this.id);return  task;}
    public CallbackResponse setTask(final TaskDefinition task){this.task=task;return this;}
}
