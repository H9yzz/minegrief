package com.chebuya.minegriefserver.jobs;

public class JobRequest {
    private String clientId;
    private String jobId;
    private String action;
    private String args;

    public JobRequest(String clientId, String jobId, String action, String args) {
        this.clientId= clientId;
        this.jobId = jobId;
        this.action = action;
        this.args = args;
    }

    public String getClientId() {
        return clientId;
    }

    public String getJobId() {
        return jobId;
    }

    public String getAction() {
        return action;
    }

    public String getArgs() {
        return args;
    }
}
