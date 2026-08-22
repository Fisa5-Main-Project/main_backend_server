package com.know_who_how.main_server.mydata.entity;

public enum MydataSyncJobStatus {
    QUEUED,
    RUNNING,
    RETRY_WAIT,
    PERSIST_RETRY,
    SUCCEEDED,
    FAILED,
    RELINK_REQUIRED
}
