package qzui.domain;

import com.github.kevinsawicki.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import restx.factory.Component;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.quartz.JobBuilder.newJob;

/**
 * Date: 19/2/14
 * Time: 06:34
 */
@Component
public class HttpJobDefinition extends AbstractJobDefinition {
    private static final Logger logger = LoggerFactory.getLogger(HttpJobDefinition.class);

    @Override
    public boolean acceptJobClass(Class<? extends Job> jobClass) {
        return jobClass.getName() == HttpJob.class.getName();
    }

    @Override
    public JobDescriptor buildDescriptor(JobDetail jobDetail, List<? extends Trigger> triggersOfJob) {
        HttpJobDescriptor jobDescriptor = setupDescriptorFromDetail(new HttpJobDescriptor(), jobDetail, triggersOfJob);

        return jobDescriptor
                .setUrl((String) jobDescriptor.getData().remove("url"))
                .setMethod((String) jobDescriptor.getData().remove("method"))
                .setBody((String) jobDescriptor.getData().remove("body"));
    }

    public static class HttpJobDescriptor extends JobDescriptor {

        private String url;
        private String method = "POST";
        private String body;
        private String contentType;
        private String login;
        private String pwdHash;

        public String getUrl() {
            return url;
        }

        public String getMethod() {
            return method;
        }

        public String getBody() {
            return body;
        }

        public String getContentType() {
            return contentType;
        }

        public String getLogin() {
            return login;
        }

        public String getPwdHash() {
            return pwdHash;
        }

        public HttpJobDescriptor setBody(final String body) {
            this.body = body;
            return this;
        }

        public HttpJobDescriptor setMethod(final String method) {
            this.method = method;
            return this;
        }

        public HttpJobDescriptor setUrl(final String url) {
            this.url = url;
            return this;
        }

        public HttpJobDescriptor setContentType(final String contentType) {
            this.contentType = contentType;
            return this;
        }

        public HttpJobDescriptor setPwdHash(final String pwdHash) {
            this.pwdHash = pwdHash;
            return this;
        }

        public HttpJobDescriptor setLogin(final String login) {
            this.login = login;
            return this;
        }

        @Override
        public JobDetail buildJobDetail() {
            JobDataMap dataMap = new JobDataMap(getData());
            dataMap.put("url", url);
            dataMap.put("method", method);
            dataMap.put("body", body);
            dataMap.put("contentType", contentType);
            dataMap.put("login", login);
            dataMap.put("pwd", pwdHash);
            return newJob(HttpJob.class)
                    .withIdentity(getName(), getGroup())
                    .usingJobData(dataMap)
                    .build();
        }

        @Override
        public String toString() {
            return "HttpJobDescriptor{" +
                    "url='" + url + '\'' +
                    ", method='" + method + '\'' +
                    ", body='" + body + '\'' +
                    ", contentType='" + contentType + '\'' +
                    '}';
        }

    }

    public static class HttpJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {

            JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
            String url = jobDataMap.getString("url");
            String method = jobDataMap.getString("method");

            try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
                HttpEntityEnclosingRequestBase request = null;
                switch (method){
                    case "DELETE": request = new HttpPut(url);break;
                    case "PUT": request = new HttpPost(url);break;
                    case "POST": request = new HttpPost(url);break;
                    case "PATCH": request = new HttpPatch(url);break;
                }
                if (!isNullOrEmpty(jobDataMap.getString("body"))) {
                    String body = jobDataMap.getString("body");
                    logger.info("Send request {} {} => {}\n{}", method, url, body);
                    StringEntity params = new StringEntity(body);
                    request.addHeader("content-type", "application/json");
                    request.setEntity(params);
                }
                HttpResponse result = httpClient.execute(request);
                String json = EntityUtils.toString(result.getEntity(), "UTF-8");
                logger.info("{} {} => {}\n{}", method, url, result.getStatusLine(), json);

            } catch (IOException ex) {
            }
        }

        private void setCrendentials(JobDataMap jobDataMap, HttpRequest request) {
            String login = jobDataMap.getString("login");
            String pwd = jobDataMap.getString("pwd");
            if (!isNullOrEmpty(login) && !isNullOrEmpty(pwd)) {
                request.basic(login, pwd);
            }
        }

        private void setContentType(JobDataMap jobDataMap, HttpRequest request) {
            String contentType = jobDataMap.getString("contentType");
            if (!isNullOrEmpty(contentType)) {
                request.contentType(contentType);
            }
        }
    }

}
