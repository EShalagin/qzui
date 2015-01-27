package qzui.dmp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by eshalagi on 1/23/15.
 */
@JsonTypeInfo( use = JsonTypeInfo.Id.NAME,  include = JsonTypeInfo.As.PROPERTY,  property = "pattern")
@JsonSubTypes({ @JsonSubTypes.Type(value = CallbackResponseCreateTask.class, name = "/api/tasks"),
        @JsonSubTypes.Type(value = CallbackResponseCreateTask.class, name = "/api/tasks/:id"),
        @JsonSubTypes.Type(value = CallbackResponseDeleteTask.class, name = "/api/tasks"),
        @JsonSubTypes.Type(value = CallbackResponseDeleteTask.class, name = "/api/tasks/:id")})
public abstract class CallbackResponse {
}
