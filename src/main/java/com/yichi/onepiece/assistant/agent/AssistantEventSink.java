package com.yichi.onepiece.assistant.agent;

import com.yichi.onepiece.assistant.dto.AssistantDto.DoneEvent;

/**
 * Where the agent reports progress. Once the client has gone away every method
 * throws {@link AssistantCancelledException}, which stops the agent.
 */
public interface AssistantEventSink {
    /** A tool is about to run. Any answer text streamed so far was a preamble; the client drops it. */
    void status(String tool, String label);

    /** A fragment of the answer as it is being written. */
    void delta(String text);

    /** The final answer, with links repaired; ends the stream. */
    void done(DoneEvent done);

    /** A failure the user should see; ends the stream. */
    void error(String code, String message);
}
