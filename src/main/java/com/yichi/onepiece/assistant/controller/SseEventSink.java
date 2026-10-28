package com.yichi.onepiece.assistant.controller;

import com.yichi.onepiece.assistant.agent.AssistantCancelledException;
import com.yichi.onepiece.assistant.agent.AssistantEventSink;
import com.yichi.onepiece.assistant.dto.AssistantDto.DeltaEvent;
import com.yichi.onepiece.assistant.dto.AssistantDto.DoneEvent;
import com.yichi.onepiece.assistant.dto.AssistantDto.ErrorEvent;
import com.yichi.onepiece.assistant.dto.AssistantDto.StatusEvent;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Writes agent events to an {@link SseEmitter}. Once the client is gone (or
 * the stream has ended) sends throw {@link AssistantCancelledException} so
 * the agent stops instead of working for nobody.
 */
final class SseEventSink implements AssistantEventSink {
    private final SseEmitter emitter;
    private volatile boolean closed;

    SseEventSink(SseEmitter emitter) {
        this.emitter = emitter;
    }

    /** Called when the emitter completes, times out or fails underneath us. */
    void markClosed() {
        closed = true;
    }

    @Override
    public void status(String tool, String label) {
        send("status", new StatusEvent(tool, label));
    }

    @Override
    public void delta(String text) {
        send("delta", new DeltaEvent(text));
    }

    @Override
    public void done(DoneEvent done) {
        send("done", done);
        complete();
    }

    @Override
    public void error(String code, String message) {
        try {
            send("error", new ErrorEvent(code, message));
        } catch (AssistantCancelledException e) {
            return; // nobody left to tell
        }
        complete();
    }

    private synchronized void send(String event, Object data) {
        if (closed) {
            throw new AssistantCancelledException();
        }
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            closed = true;
            throw new AssistantCancelledException();
        }
    }

    private synchronized void complete() {
        if (!closed) {
            closed = true;
            emitter.complete();
        }
    }
}
