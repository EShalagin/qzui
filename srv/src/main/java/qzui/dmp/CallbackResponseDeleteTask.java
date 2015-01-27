package qzui.dmp;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by eshalagi on 1/23/15.
 */
public class CallbackResponseDeleteTask extends   CallbackResponse {
    private String id;
    public String getId() { return id; }
    public CallbackResponse setId(final String id) { this.id=id; return  this; }


    @JsonProperty("data")
    private TaskDefinition task;
    public TaskDefinition getTask(){task.setId(this.id);return  task;}
    public CallbackResponse setTask(final TaskDefinition task){this.task=task;return this;}
}
