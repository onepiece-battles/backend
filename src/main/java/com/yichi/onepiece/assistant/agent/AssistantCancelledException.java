package com.yichi.onepiece.assistant.agent;

/** The client went away mid-answer; the agent stops quietly instead of reporting an error. */
public class AssistantCancelledException extends RuntimeException {
    public AssistantCancelledException() {
        super("assistant stream closed by the client", null, false, false);
    }
}
