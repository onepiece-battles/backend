package com.yichi.onepiece.assistant.importing;

import com.yichi.onepiece.model.I18nText;
import java.util.Map;

/** Facts about a One Piece character from an outside source, by the source's field name. */
@FunctionalInterface
public interface CharacterFacts {
  /** What the source knows about the character with this name; empty when nothing. Never throws. */
  Map<String, String> find(I18nText name);
}
