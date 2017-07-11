package com.apifortress.jenkins.plugin;
import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The API Fortress Jenkins Plugin
 */
public class ApiFortressBuilder extends Builder{

    /**
     * The Hook URL
     */
    private final String hook;
    /**
     * singleTest, automatch, tag, project
     */
    private final String mode;
    /**
     * can either be the test ID, the automatch URL or the tag
     */
    private final String id;
    /**
     * true if you want to return a build step failure on test failure
     */
    private final boolean blocking;
    /**
     * true if you don't want to store the event in the API Fortress dashboard
     */
    private final boolean dryrun;
    /**
     * true if you don't want notifications to be sent
     */
    private final boolean silent;
    /**
     * first couple of override parameters
     */
    private final String param1name,param1value;
    /**
     * second couple of override parameters
     */
    private final String param2name,param2value;
    /**
     * third couple of override parameters
     */
    private final String param3name,param3value;

    static Logger log = Logger.getLogger("com.apifortress.jenkins.plugin.ApiFortressBuilder");

    /**
     * Constructor
     * @param mode the mode
     * @param hook the Hook URL
     * @param id test id, automatch url or tag
     * @param blocking true if this is a blocking build step
     * @param dryrun true if you don't want to store the even on API Fortress
     * @param silent true if you don't want to trigger notifications
     * @param param1name override param 1 name
     * @param param1value override param 1 value
     * @param param2name override param 2 name
     * @param param2value override param 2 value
     * @param param3name override param 3 name
     * @param param3value override param 3 value
     */
    @DataBoundConstructor
    public ApiFortressBuilder(String mode, String hook, String id, boolean blocking, boolean dryrun,
                              boolean silent,
                              String param1name, String param1value,
                              String param2name, String param2value,
                              String param3name, String param3value) {
        this.mode = mode;
        this.hook = hook;
        this.id = id;
        this.blocking = blocking;
        this.dryrun = dryrun;
        this.silent = silent;
        this.param1name = param1name;
        this.param1value = param1value;
        this.param2name = param2name;
        this.param2value = param2value;
        this.param3name = param3name;
        this.param3value = param3value;

    }


    public String getHook() {
        return hook;
    }
    public String getMode() {
        return mode;
    }

    public String getId(){
        return id;
    }
    public boolean isBlocking(){
        return blocking;
    }
    public boolean isDryrun(){
        return dryrun;
    }
    public boolean isSilent(){
        return silent;
    }

    public String getParam1name(){
        return param1name;
    }
    public String getParam1value(){
        return param1value;
    }

    public String getParam2name(){
        return param2name;
    }
    public String getParam2value() {
        return param2value;
    }
    public String getParam3name(){
        return param3name;
    }
    public String getParam3value(){
        return param3value;
    }

    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"payload\":\"{}\",\"Content-Type\":\"application/json\",");
        if(getMode().equals("automatch"))
            sb.append("\"url\":\""+getId()+"\",");
        sb.append("\"params\":"+buildParams());
        sb.append("}");

        StringBuilder theUrl = new StringBuilder();
        theUrl.append(getHook()+"/tests/");
        if(getMode().equals("singleTest"))
            theUrl.append(id+"/run");
        if(getMode().equals("automatch"))
            theUrl.append("automatch");
        if(getMode().equals("tag"))
            theUrl.append("tag/"+id+"/run");
        if(getMode().equals("project"))
            theUrl.append("run-all");
        theUrl.append("?");
        theUrl.append("sync="+isBlocking());
        theUrl.append("&dryrun="+isDryrun());
        theUrl.append("&silent="+isSilent());
        String url = theUrl.toString();
        boolean failed = true;
        InputStreamReader is = null;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type","application/json");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(sb.toString());
            wr.flush();
            wr.close();
            int responseCode = connection.getResponseCode();
            if(responseCode==200) {
                is = new InputStreamReader(connection.getInputStream(),"UTF-8");
                Object response = new JsonSlurper().parse(is);
                if (response instanceof ArrayList) {
                    failed = hasFailures((ArrayList) response);
                } else {
                    failed = hasFailures((Map) response);
                }
                is.close();
            } else {
                listener.fatalError("API Fortress server responded with a "+responseCode+" status code");
                log.warning("API Fortress server responded with a "+responseCode+" status code. No test executed");
            }

        }catch(Exception e) {
            listener.fatalError("Error during API testing. Reason "+e.getMessage());
            log.log(Level.SEVERE,"Error during API testing",e);
        }
        finally{
            try {
                is.close();
            } catch(Exception e){}
        }

        return (blocking && !failed) || (!blocking);
    }

    /**
     * Returns true when at least one of the tests has a failure
     * @param items an array of tests results
     * @return true if at least one test failed
     */
    private boolean hasFailures(ArrayList items){
        Iterator<Map> iterator =  items.iterator();
        while(iterator.hasNext()){
            if(hasFailures(iterator.next()))
                return true;
        }
        return false;
    }

    /**
     * Returns true when the test result contains a failure
     * @param item a test result
     * @return true if the test failued
     */
    private boolean hasFailures(Map item){
        if(item.containsKey("code") && item.get("code").equals("ACCEPTED"))
            return false;
        return ((Integer)item.get("failuresCount")) > 0;
    }

    /**
     * Build a map of override parameters
     * @return a map of override parameters
     */
    private String buildParams(){
        HashMap<String,String> items = new HashMap<>();
        if(paramValid(param1name,param1value))
            items.put(param1name,param1value);
        if(paramValid(param2name,param2value))
            items.put(param2name,param2value);
        if(paramValid(param3name,param3value))
            items.put(param3name,param3value);
        return JsonOutput.toJson(items);
    }

    /**
     * Validates a parameter name+value
     * @param paramName the parameter name
     * @param paramValue the parameter value
     * @return true if the parameter has been validated
     */
    private static boolean paramValid(String paramName,String paramValue){
        return paramName!=null&&paramName.length()>0&&paramValue!=null&&paramValue.length()>0;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        public FormValidation doCheckHook(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a Hook URL");
            if(value.length()>0) try{
                new URL(value);
            }catch(Exception e){
                FormValidation.error("Please enter a valid URL");
            }
            return FormValidation.ok();
        }
        public FormValidation doCheckId(@QueryParameter("id")String value, @QueryParameter("mode") String mode)
                throws IOException, ServletException {
            if(mode.equals("singleTest") || mode.equals("automatch") || mode.equals("tag"))
                if (value.length() == 0)
                    return FormValidation.error("Please enter a valid test ID or automatch pattern");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "API Fortress API Testing";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            return super.configure(req,formData);
        }
    }
}

