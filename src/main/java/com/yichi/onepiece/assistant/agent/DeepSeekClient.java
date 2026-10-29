package com.yichi.onepiece.assistant.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Streams chat completions from DeepSeek ({@code stream: true}) with the
 * server-held key. Answer text is forwarded as it arrives so the client can
 * render the reply while it is still being written.
 */
@Component
public class DeepSeekClient implements ChatCompletionClient {
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    // Time to the first byte only; the streamed body is bounded by the SSE emitter timeout.
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_LOGGED_ERROR_BODY = 500;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

    @Override
    public Completion complete(CompletionRequest request, Consumer<String> onContentDelta) {
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(request.baseUrl()))
                .timeout(RESPONSE_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer " + request.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(encode(request), StandardCharsets.UTF_8))
                .build();
        HttpResponse<Stream<String>> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines());
        } catch (HttpTimeoutException e) {
            throw new AssistantException(AssistantException.TIMEOUT, "DeepSeek did not respond in time", e);
        } catch (IOException e) {
            throw new AssistantException(AssistantException.UPSTREAM_ERROR, "Could not reach DeepSeek", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssistantCancelledException();
        }

        // Closing the line stream aborts the HTTP exchange, including when the
        // client disconnects and onContentDelta throws AssistantCancelledException.
        try (Stream<String> lines = response.body()) {
            if (response.statusCode() != 200) {
                String detail = lines.collect(Collectors.joining("\n"));
                logger.warn("DeepSeek returned HTTP {}: {}",
                        response.statusCode(), StringUtils.abbreviate(detail, MAX_LOGGED_ERROR_BODY));
                throw new AssistantException(
                        AssistantException.UPSTREAM_ERROR, "DeepSeek returned HTTP " + response.statusCode());
            }
            DeepSeekStreamAccumulator accumulator = new DeepSeekStreamAccumulator(objectMapper);
            Iterator<String> iterator = lines.iterator();
            while (!accumulator.isDone() && iterator.hasNext()) {
                String text = accumulator.accept(iterator.next());
                if (text != null) {
                    onContentDelta.accept(text);
                }
            }
            return accumulator.result();
        } catch (UncheckedIOException e) {
            throw new AssistantException(AssistantException.UPSTREAM_ERROR, "DeepSeek stream was interrupted", e);
        }
    }

    private String encode(CompletionRequest request) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", request.model());
        ArrayNode messages = body.putArray("messages");
        request.messages().forEach(messages::add);
        if (request.tools() != null && !request.tools().isEmpty()) {
            body.set("tools", request.tools());
            body.put("tool_choice", "auto");
        }
        if (request.jsonOutput()) {
            body.putObject("response_format").put("type", "json_object");
        }
        body.put("temperature", request.temperature());
        body.put("max_tokens", request.maxTokens());
        body.put("stream", true);
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new AssistantException(
                    AssistantException.INTERNAL_ERROR, "Could not encode the DeepSeek request", e);
        }
    }
}
