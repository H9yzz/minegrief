package com.chebuya.minegriefserver.jobs;

public class JobOutput {
    private String clientId;
    private String jobId;
    private String action;
    private String output;

    public JobOutput(String clientId, String jobId, String action, String output) {
        this.clientId = clientId;
        this.jobId = jobId;
        this.action = action;
        this.output = output;
    }

    public String getClientId() {
        return clientId;
    }

    public String getOutput() {
        return output;
    }

    public String getJobId() {
        return jobId;
    }
}
