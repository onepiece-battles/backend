package com.yichi.onepiece.assistant.agent;

import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Repairs the {@code onepiece://TYPE/ID} links in an answer against the
 * resources the tools actually returned, so a hallucinated id never becomes a
 * tappable link. Server-side port of the client's former normalizeResourceLinks.
 */
public final class ResourceLinkNormalizer {
    private static final Set<String> LINKABLE_TYPES = Set.of("character", "fruit", "group", "battle");
    // Markdown links and bare URLs in one pass, so a URL inside a repaired
    // Markdown link can never be wrapped a second time.
    private static final Pattern TOKENS = Pattern.compile(
            "\\[([^\\]]*)\\]\\(\\s*onepiece://([A-Za-z_]+)/([^\\s)]+)\\s*\\)"
                    + "|onepiece://([A-Za-z_]+)/([^\\s)\\]，。！？；：,.;!?`*]+)",
            Pattern.CASE_INSENSITIVE);

    private ResourceLinkNormalizer() {}

    public static String normalize(String content, Collection<ResourceLink> resources) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        Map<String, ResourceLink> unique = new LinkedHashMap<>();
        for (ResourceLink resource : resources) {
            if (resource != null && LINKABLE_TYPES.contains(resource.type())
                    && resource.id() != null && !resource.id().isEmpty()) {
                unique.putIfAbsent(resource.type() + "/" + resource.id(), resource);
            }
        }
        List<ResourceLink> known = List.copyOf(unique.values());

        Matcher matcher = TOKENS.matcher(content);
        StringBuilder normalized = new StringBuilder();
        while (matcher.find()) {
            String type = (matcher.group(2) != null ? matcher.group(2) : matcher.group(4)).toLowerCase(Locale.ROOT);
            String id = matcher.group(3) != null ? matcher.group(3) : matcher.group(5);
            matcher.appendReplacement(normalized, Matcher.quoteReplacement(link(type, id, matcher.group(1), known)));
        }
        matcher.appendTail(normalized);
        return normalized.toString();
    }

    private static String link(String type, String id, String label, List<ResourceLink> known) {
        ResourceLink reference = known.stream()
                .filter(r -> r.type().equals(type) && r.id().equals(id))
                .findFirst()
                .orElse(null);
        // Repair a mistaken id only when the visible name identifies exactly one
        // known resource. Never make an invented id navigable.
        if (reference == null && label != null) {
            String wanted = label.trim().toLowerCase(Locale.ROOT);
            List<ResourceLink> matches = known.stream()
                    .filter(r -> r.type().equals(type) && r.name() != null
                            && r.name().trim().toLowerCase(Locale.ROOT).equals(wanted))
                    .toList();
            if (matches.size() == 1) {
                reference = matches.get(0);
            }
        }
        String display = label != null && !label.isBlank() && !label.toLowerCase(Locale.ROOT).contains("onepiece:")
                ? label.trim()
                : reference != null ? reference.name() : type;
        if (reference == null) {
            return display;
        }
        return "[" + display + "](onepiece://" + reference.type() + "/" + reference.id() + ")";
    }
}
