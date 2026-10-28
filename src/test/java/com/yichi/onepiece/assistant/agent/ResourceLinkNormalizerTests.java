package com.yichi.onepiece.assistant.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import java.util.List;
import org.testng.annotations.Test;

public class ResourceLinkNormalizerTests {
    private static final ResourceLink LUFFY = new ResourceLink("character", "c-luffy", "Luffy");
    private static final ResourceLink GOMU = new ResourceLink("fruit", "f-gomu", "Gomu Gomu no Mi");

    @Test
    public void keepsLinksToResourcesTheToolsReturned() {
        String content = "[Luffy](onepiece://character/c-luffy) ate the [Gomu Gomu no Mi](onepiece://fruit/f-gomu).";

        assertThat(ResourceLinkNormalizer.normalize(content, List.of(LUFFY, GOMU))).isEqualTo(content);
    }

    @Test
    public void repairsAWrongIdWhenTheNameIsUnambiguous() {
        assertThat(ResourceLinkNormalizer.normalize("[Luffy](onepiece://character/wrong-id) wins", List.of(LUFFY)))
                .isEqualTo("[Luffy](onepiece://character/c-luffy) wins");
    }

    @Test
    public void neverMakesAnInventedResourceTappable() {
        assertThat(ResourceLinkNormalizer.normalize("[Zoro](onepiece://character/c-zoro) is strong.", List.of(LUFFY)))
                .isEqualTo("Zoro is strong.");
    }

    @Test
    public void doesNotGuessBetweenTwoResourcesWithTheSameName() {
        var anotherLuffy = new ResourceLink("character", "c-luffy-2", "Luffy");

        assertThat(ResourceLinkNormalizer.normalize("[Luffy](onepiece://character/x)", List.of(LUFFY, anotherLuffy)))
                .isEqualTo("Luffy");
    }

    @Test
    public void turnsABareUrlIntoANamedLink() {
        assertThat(ResourceLinkNormalizer.normalize("See onepiece://character/c-luffy.", List.of(LUFFY)))
                .isEqualTo("See [Luffy](onepiece://character/c-luffy).");
    }

    @Test
    public void leavesPlainTextAlone() {
        assertThat(ResourceLinkNormalizer.normalize("No links here.", List.of(LUFFY))).isEqualTo("No links here.");
    }
}
