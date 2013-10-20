package org.jboss.processFlow.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import org.apache.log4j.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.commons.io.IOUtils;
import org.apache.http.util.EntityUtils;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

import org.jboss.processFlow.knowledgeService.IKnowledgeSession;
import org.jboss.processFlow.test.SyncClientTest;

public abstract class BaseRestClient extends BaseClient {

    protected static final Logger log = Logger.getLogger(BaseRestClient.class);
    public static final String PROTOCOL_KEY = "org.jboss.processFlow.rest.client.protocol";
    public static final String HOST_KEY = "org.jboss.processFlow.rest.client.host"; 
    public static final String PORT_KEY = "org.jboss.processFlow.rest.client.port";
    public static final String KSERVICE_PORT_KEY = "org.jboss.processFlow.rest.client.kservice.port";
    public static final String USR_KEY = "org.jboss.processFlow.rest.client.usr";
    public static final String PWD_KEY = "org.jboss.processFlow.rest.client.pwd";
    public static final String PACKAGES_KEY = "org.jboss.processFlow.rest.client.packages";
    public static final String SUBDOMAIN_KEY = "org.jboss.processFlow.rest.client.subdomain";
    public static final String CONNECTTIMEOUT_KEY = "org.jboss.processFlow.rest.client.connect.timeout";
    public static final String READTIMEOUT_KEY = "org.jboss.processFlow.rest.client.read.timeout";
    public static final String SNAPSHOT_NAME = "org.jboss.processFlow.rest.client.snapshot.name";
    public static final String USE_FORM_URL = "org.jboss.processFlow.rest.client.useFormUrl";
    public static final String DELIVER_ASYNC = "org.jboss.processFlow.rest.client.deliverAsync";
    protected static final String SUBDOMAIN_STATUS="/rs/server/status";
    protected static final String SUBDOMAIN_NEW_INSTANCE="/rs/process/definition/";
    protected static final String SUBDOMAIN_FORM_BASED_NEW_INSTANCE="/rs/form/process/";
    protected static final String SUBDOMAIN_SID="/rs/identity/secure/sid";
    protected static final String SUBDOMAIN_J_SECURITY="/rs/identity/secure/j_security_check";
    protected static final String SEPERATOR="://";
    protected static final String SUBDOMAIN_TASKS="/rs/tasks/";
    protected static final String SUBDOMAIN_TASK="/rs/task/";
    protected static final String SUBDOMAIN_SIGNAL="/rs/process/tokens/";
    protected static final String ID="id";
    protected static final String DEFINITION_ID="definitionId";
    protected static final String START_DATE="startDate";
    protected static final String SIGNAL="signal";
    
    protected String urlPrefix;
    protected String userId;
    protected String password;
    protected DefaultHttpClient httpclient;
    protected ObjectMapper jsonMapper;
    protected boolean useForm;
    protected boolean deliverAsync;

    public BaseRestClient(Integer id) throws IOException {
        super(id);

        String protocol = properties.getProperty(PROTOCOL_KEY, "http");
        String host = properties.getProperty(HOST_KEY, "localhost");
        String port = properties.getProperty(PORT_KEY, "8080");
        String subdomain = properties.getProperty(SUBDOMAIN_KEY, "business-central-server");
        userId = properties.getProperty(USR_KEY, "jboss");
        password = properties.getProperty(PWD_KEY, "brms");
        useForm = Boolean.parseBoolean(properties.getProperty(USE_FORM_URL, "TRUE"));
        deliverAsync = Boolean.parseBoolean(properties.getProperty(DELIVER_ASYNC, "FALSE"));
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append(protocol);
        sBuilder.append(SEPERATOR);
        sBuilder.append(host);
        sBuilder.append(":");
        sBuilder.append(port);
        sBuilder.append("/"+subdomain);
        urlPrefix = sBuilder.toString();
        jsonMapper = new ObjectMapper();

        processId = properties.getProperty("org.jboss.processFlow.rest.client.process.id", "simpleTask");
    }
    
    protected String processResponse(HttpResponse response, String targetUrl, int desiredStatus) throws Exception{
        HttpEntity httpEntity = null;
        StringBuilder returnLog;
        int returnedStatus;
        try {
            httpEntity = response.getEntity();
            InputStream contentStream = httpEntity.getContent();
            String content = IOUtils.toString(contentStream);
            returnedStatus = response.getStatusLine().getStatusCode();
            if(SyncClientTest.enableLog){
                returnLog = new StringBuilder("\nprocessResponse() targetUrl = "+targetUrl+"\n\tstatus :\n\t\t");
                returnLog.append(returnedStatus);
                returnLog.append("\n\tcookies : \n\t\t");
                httpEntity = response.getEntity();
                List<Cookie> cookies = httpclient.getCookieStore().getCookies();
                if (cookies.isEmpty()) {
                    returnLog.append("no cookies");
                } else {
                    for (int i = 0; i < cookies.size(); i++) {
                        returnLog.append("- " + cookies.get(i).toString()+"\n");
                    }
                }
                returnLog.append("\n\t content :\n\t\t");
                returnLog.append(content);
                returnLog.append("\n");
                System.out.println(returnLog.toString());
            }
            contentStream.close();
            if(desiredStatus != 0 && (desiredStatus != returnedStatus)){
                throw new Exception("processResponse() returnedStatus = "+returnedStatus+" for target URL = "+targetUrl);
            }
            return content;
        } finally{
            if(httpEntity != null)
                EntityUtils.consume(httpEntity);
        }
    }

    protected void authenticateIntoBusinessCentral() throws Exception {
        String targetUrl = urlPrefix+SUBDOMAIN_SID;
        HttpGet httpGet = new HttpGet(targetUrl);
        HttpResponse response = httpclient.execute(httpGet);
        processResponse(response, targetUrl, HttpStatus.SC_OK);
        
        targetUrl = urlPrefix+SUBDOMAIN_J_SECURITY;
        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("j_username", userId));
        nvps.add(new BasicNameValuePair("j_password", password));
        HttpPost httpPost = new HttpPost(targetUrl);
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        response = httpclient.execute(httpPost);
        processResponse(response, targetUrl, HttpStatus.SC_MOVED_TEMPORARILY);
        
        targetUrl = urlPrefix+SUBDOMAIN_SID;
        httpGet = new HttpGet(targetUrl);
        response = httpclient.execute(httpGet);
        processResponse(response, targetUrl, HttpStatus.SC_OK);
        
    }
}
