package com.orchestraj.workflow;

public enum AsyncWorkflowState {
    PENDING,
    RUNNING,
    COMPLETED,
    TIMED_OUT,
    CANCELED,
    FAILED
}
