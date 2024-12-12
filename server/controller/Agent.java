package com.chebuya.minegriefserver.controller;

public class Agent {
    public String uuid;
    public String hostname;
    public String user;
    public String ip;

    public Agent(String uuid, String hostname, String user, String ip) {
        this.uuid = uuid;
        this.hostname = hostname;
        this.user = user;
        this.ip = ip;
    }
}
