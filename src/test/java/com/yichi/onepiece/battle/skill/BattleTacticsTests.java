package com.yichi.onepiece.battle.skill;

import static com.yichi.onepiece.battle.skill.CombatModels.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.assistant.agent.ChatCompletionClient;
import com.yichi.onepiece.assistant.agent.ChatCompletionClient.*;
import com.yichi.onepiece.assistant.service.AiSettingService;
import java.util.*;
import org.testng.annotations.Test;

public class BattleTacticsTests {
  @Test
  public void aiCanRankOnlyFrozenSkillsAndCannotSupplyDamageOrAnotherActor() {
    var settings = mock(AiSettingService.class);
    when(settings.isConfigured()).thenReturn(true);
    when(settings.apiKey()).thenReturn("test");
    when(settings.baseUrl()).thenReturn("https://api.test");
    when(settings.model()).thenReturn("fixture");
    var id = UUID.randomUUID();
    var profile = new CombatCatalog().catalog().characters().get(0).profile();
    var legal = profile.skills().get(0).id();
    var client = mock(ChatCompletionClient.class);
    when(client.complete(any(), any()))
        .thenReturn(
            new Completion(
                "{\"priorities\":{\""
                    + id
                    + "\":[\"invented\",\""
                    + legal
                    + "\",\""
                    + legal
                    + "\"],\"another actor\":[\"kill\"]},\"damage\":999999}",
                List.of(),
                Usage.NONE));
    try (var close = new TacticsClose(new BattleTactics(settings, client, true))) {
      var result = close.value.plan(Map.of(id, profile), Map.of(id, Map.of("energy", 250)));
      assertThat(result.source()).isEqualTo("AI");
      assertThat(result.priorities()).containsOnlyKeys(id.toString());
      assertThat(result.priorities().get(id.toString())).containsExactly(legal);
      verify(client, times(1)).complete(any(), any());
    }
  }

  @Test
  public void unavailableOrMalformedAiFallsBackWithoutChangingTheProfile() {
    var settings = mock(AiSettingService.class);
    when(settings.isConfigured()).thenReturn(true);
    var profile = new CombatCatalog().catalog().characters().get(0).profile();
    var client = mock(ChatCompletionClient.class);
    when(client.complete(any(), any()))
        .thenThrow(new IllegalStateException("provider unavailable"));
    try (var close = new TacticsClose(new BattleTactics(settings, client, true))) {
      assertThat(close.value.plan(Map.of(UUID.randomUUID(), profile), Map.of()).source())
          .isEqualTo("RULES");
    }
    assertThat(profile.skills()).isNotEmpty();
  }

  @Test
  public void disabledAiAndGenericActorsDoNotMakePaidRequests() {
    var settings = mock(AiSettingService.class);
    var client = mock(ChatCompletionClient.class);
    try (var close = new TacticsClose(new BattleTactics(settings, client, false))) {
      assertThat(close.value.plan(Map.of(), Map.of()).source()).isEqualTo("RULES");
    }
    try (var close = new TacticsClose(new BattleTactics(settings, client, true))) {
      assertThat(
              close.value.plan(Map.of(UUID.randomUUID(), CombatCatalog.basic()), Map.of()).source())
          .isEqualTo("RULES");
    }
    verifyNoInteractions(settings, client);
  }

  @Test
  public void slowAiIsInterruptedAtTheDeadlineAndRulesCanContinue() throws Exception {
    var settings = mock(AiSettingService.class);
    when(settings.isConfigured()).thenReturn(true);
    var client = mock(ChatCompletionClient.class);
    var interrupted = new java.util.concurrent.CountDownLatch(1);
    when(client.complete(any(), any()))
        .thenAnswer(
            call -> {
              try {
                new java.util.concurrent.CountDownLatch(1).await();
              } catch (InterruptedException e) {
                interrupted.countDown();
                throw e;
              }
              return null;
            });
    try (var close = new TacticsClose(new BattleTactics(settings, client, true, 300))) {
      long start = System.nanoTime();
      var profile = new CombatCatalog().catalog().characters().get(0).profile();
      assertThat(close.value.plan(Map.of(UUID.randomUUID(), profile), Map.of()).source())
          .isEqualTo("RULES");
      assertThat(java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start))
          .isLessThan(2000);
      assertThat(interrupted.await(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    }
  }

  private record TacticsClose(BattleTactics value) implements AutoCloseable {
    public void close() {
      value.close();
    }
  }
}
