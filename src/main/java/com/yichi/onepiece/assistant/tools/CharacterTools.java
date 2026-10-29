package com.yichi.onepiece.assistant.tools;

import static com.yichi.onepiece.assistant.tools.ToolJson.singleMatch;

import static com.yichi.onepiece.assistant.tools.ToolJson.link;
import static com.yichi.onepiece.assistant.tools.ToolJson.map;
import static com.yichi.onepiece.assistant.tools.ToolJson.nonNull;
import static com.yichi.onepiece.assistant.tools.ToolJson.result;
import static com.yichi.onepiece.assistant.tools.ToolJson.text;

import com.yichi.onepiece.assistant.agent.AssistantLanguage;
import com.yichi.onepiece.assistant.agent.AssistantTool;
import com.yichi.onepiece.assistant.agent.AssistantToolProvider;
import com.yichi.onepiece.assistant.agent.ToolContext;
import com.yichi.onepiece.assistant.agent.ToolResult;
import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.BattleListView;
import com.yichi.onepiece.model.BattleStatus;
import com.yichi.onepiece.model.CharacterFilter;
import com.yichi.onepiece.model.CharacterFullView;
import com.yichi.onepiece.model.CharacterListView;
import com.yichi.onepiece.model.CharacterSortableField;
import com.yichi.onepiece.model.CharacterSortableFieldsEnum;
import com.yichi.onepiece.model.FruitListView;
import com.yichi.onepiece.model.GroupForCharacterView;
import com.yichi.onepiece.model.Haki;
import com.yichi.onepiece.model.ListCharacterRequest;
import com.yichi.onepiece.model.ListCharacterResponse;
import com.yichi.onepiece.model.Permission;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.model.SortOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Character lookups (search, detail, power ranking) and the battle proposal. */
@Component
public class CharacterTools implements AssistantToolProvider {
    static final int SEARCH_LIMIT = 6;
    static final int BATTLE_HISTORY_LIMIT = 8;
    static final String BATTLE_PROPOSAL = "battle_proposal";

    private final CharacterService characterService;
    private final IamService iamService;

    public CharacterTools(CharacterService characterService, IamService iamService) {
        this.characterService = characterService;
        this.iamService = iamService;
    }

    @Override
    public List<AssistantTool> tools() {
        return List.of(
                new AssistantTool("search_characters",
                        "Search characters by (partial) name. Returns id, name, combat power, bounty and haki.",
                        ToolJson.schema("""
                                {"type": "object",
                                 "properties": {"name": {"type": "string", "description": "Character name or part of it"}},
                                 "required": ["name"]}"""),
                        (arguments, context) -> searchCharacters(text(arguments, "name"), context),
                        (arguments, language) -> language.pick(
                                "正在搜索角色「" + text(arguments, "name") + "」",
                                "Searching characters \"" + text(arguments, "name") + "\"")),
                new AssistantTool("get_character_detail",
                        "Full profile of a character by name: all combat stats, devil fruits, crews and "
                                + "battle history (opponent + win/lose).",
                        ToolJson.schema("""
                                {"type": "object", "properties": {"name": {"type": "string"}}, "required": ["name"]}"""),
                        (arguments, context) -> characterDetail(text(arguments, "name"), context),
                        (arguments, language) -> language.pick(
                                "正在查询角色「" + text(arguments, "name") + "」的资料",
                                "Looking up " + text(arguments, "name"))),
                new AssistantTool("list_top_characters",
                        "List the strongest characters by combat power, highest first.",
                        ToolJson.schema("""
                                {"type": "object",
                                 "properties": {"limit": {"type": "integer", "description": "How many, 1-10"}},
                                 "required": []}"""),
                        (arguments, context) -> topCharacters(ToolJson.integer(arguments, "limit", 5), context),
                        (arguments, language) -> language.pick("正在查看战力排行", "Checking the power ranking")),
                new AssistantTool("propose_battle",
                        "Propose a battle between two characters so the user can create it. Call this when "
                                + "the user wants to simulate or create a fight (e.g. \"who would win, A vs B\", "
                                + "\"create a battle between A and B\").",
                        ToolJson.schema("""
                                {"type": "object",
                                 "properties": {"starter_name": {"type": "string"}, "receiver_name": {"type": "string"}},
                                 "required": ["starter_name", "receiver_name"]}"""),
                        (arguments, context) -> proposeBattle(
                                text(arguments, "starter_name"), text(arguments, "receiver_name"), context),
                        (arguments, language) -> language.pick(
                                "正在准备对战:" + text(arguments, "starter_name") + " vs " + text(arguments, "receiver_name"),
                                "Preparing a battle: " + text(arguments, "starter_name") + " vs "
                                        + text(arguments, "receiver_name"))));
    }

    ToolResult searchCharacters(String name, ToolContext context) {
        checkList();
        AssistantLanguage language = context.language();
        List<ResourceLink> links = new ArrayList<>();
        List<Map<String, Object>> characters = new ArrayList<>();
        for (CharacterListView character : list(name, context, SEARCH_LIMIT, List.of())) {
            String displayName = language.displayName(character.getName());
            links.add(link("character", character.getId(), displayName));
            characters.add(map("id", character.getId(), "name", displayName,
                    "combat_power", character.getScore(), "bounty", character.getBounty(),
                    "haki", hakis(character.getHakis())));
        }
        return result(map("characters", characters), links);
    }

    ToolResult characterDetail(String name, ToolContext context) {
        checkList();
        // Same permissions as the character detail endpoint, which also exposes battles, fruits and crews.
        for (ResourceType type : List.of(ResourceType.CHARACTER, ResourceType.BATTLE, ResourceType.FRUIT, ResourceType.GROUP)) {
            iamService.checkPermissionOfCurrentAccount(type, Set.of(Permission.GET));
        }
        Optional<CharacterListView> match = findOne(name, context);
        if (match.isEmpty()) {
            return result(map("found", false, "query", name), List.of());
        }
        CharacterFullView full = characterService.getCharacterFullView(match.get().getId());
        if (full == null) {
            return result(map("found", false, "query", name), List.of());
        }
        AssistantLanguage language = context.language();
        String characterName = language.displayName(full.getName());
        List<ResourceLink> links = new ArrayList<>();
        links.add(link("character", full.getId(), characterName));

        List<Map<String, Object>> fruits = new ArrayList<>();
        for (FruitListView fruit : nonNull(full.getFruits())) {
            String fruitName = language.displayName(fruit.getName());
            links.add(link("fruit", fruit.getId(), fruitName));
            fruits.add(map("id", fruit.getId(), "name", fruitName));
        }

        List<Map<String, Object>> crews = new ArrayList<>();
        for (GroupForCharacterView crew : nonNull(full.getGroups())) {
            String crewName = language.displayName(crew.getName());
            if (crew.getGroupId() != null) {
                links.add(link("group", crew.getGroupId(), crewName));
            }
            crews.add(map("id", crew.getGroupId(), "name", crewName));
        }

        List<Map<String, Object>> battles = new ArrayList<>();
        for (BattleListView battle : nonNull(full.getBattles()).stream().limit(BATTLE_HISTORY_LIMIT).toList()) {
            boolean isStarter = full.getId().equals(battle.getStarterId());
            var opponent = isStarter ? battle.getReceiver() : battle.getStarter();
            UUID opponentId = opponent == null ? null : opponent.getId();
            String opponentName = opponent == null ? "" : language.displayName(opponent.getName());
            var execution = battle.getBattleExecution();
            UUID winnerId = execution == null ? null : execution.getWinnerId();
            // FAILED marks a draw: the battle reached the turn limit.
            String outcome = execution != null && execution.getBattleStatus() == BattleStatus.FAILED ? "draw"
                    : winnerId == null ? "undecided" : winnerId.equals(full.getId()) ? "win" : "lose";
            if (opponentId != null) {
                links.add(link("character", opponentId, opponentName));
            }
            if (battle.getId() != null) {
                links.add(link("battle", battle.getId(),
                        opponentName.isEmpty() ? characterName : "vs " + opponentName));
            }
            battles.add(map("id", battle.getId(),
                    "opponent", map("id", opponentId, "name", opponentName),
                    "result", outcome));
        }

        return result(map(
                "id", full.getId(),
                "name", characterName,
                "combat_power", full.getScore(),
                "bounty", full.getBounty(),
                "stats", map(
                        "attack", full.getAttack(),
                        "defense", full.getDefense(),
                        "intelligence", full.getIntelligence(),
                        "blood", full.getBlood(),
                        "speed", full.getSpeed(),
                        "group_gain", full.getGroupGain(),
                        "energy", full.getEnergy()),
                "haki", hakis(full.getHakis()),
                "devil_fruits", fruits,
                "crews", crews,
                "battles", battles), links);
    }

    ToolResult topCharacters(int limit, ToolContext context) {
        checkList();
        int size = Math.max(1, Math.min(10, limit));
        CharacterSortableField byScore = new CharacterSortableField()
                .sortKey(CharacterSortableFieldsEnum.SCORE)
                .sortOrder(SortOrder.DESC);
        AssistantLanguage language = context.language();
        List<ResourceLink> links = new ArrayList<>();
        List<Map<String, Object>> characters = new ArrayList<>();
        for (CharacterListView character : list(null, context, size, List.of(byScore))) {
            String displayName = language.displayName(character.getName());
            links.add(link("character", character.getId(), displayName));
            characters.add(map("id", character.getId(), "name", displayName,
                    "combat_power", character.getScore(), "bounty", character.getBounty()));
        }
        return result(map("top_characters", characters), links);
    }

    ToolResult proposeBattle(String starterName, String receiverName, ToolContext context) {
        checkList();
        Optional<CharacterListView> starter = findOne(starterName, context);
        Optional<CharacterListView> receiver = findOne(receiverName, context);
        if (starter.isEmpty() || receiver.isEmpty()) {
            return result(map("proposed", false,
                    "starter_found", starter.isPresent(), "receiver_found", receiver.isPresent()), List.of());
        }
        AssistantLanguage language = context.language();
        String starterDisplay = language.displayName(starter.get().getName());
        String receiverDisplay = language.displayName(receiver.get().getName());
        List<ResourceLink> links = List.of(
                link("character", starter.get().getId(), starterDisplay),
                link("character", receiver.get().getId(), receiverDisplay),
                // Not a link: the client turns it into a Confirm/Cancel card (id = "starter|receiver").
                new ResourceLink(BATTLE_PROPOSAL, starter.get().getId() + "|" + receiver.get().getId(),
                        starterDisplay + " vs " + receiverDisplay));
        return result(map(
                "proposed", true,
                "starter", map("id", starter.get().getId(), "name", starterDisplay),
                "receiver", map("id", receiver.get().getId(), "name", receiverDisplay),
                "note", "A battle proposal card with Confirm/Cancel is shown to the user. "
                        + "Do not output a onepiece:// link for this; just briefly say you prepared it."), links);
    }

    private void checkList() {
        iamService.checkPermissionOfCurrentAccount(ResourceType.CHARACTER, Set.of(Permission.LIST));
    }

    private Optional<CharacterListView> findOne(String name, ToolContext context) {
        return singleMatch(list(name, context, 30, List.of()), name, "character", CharacterListView::getId, CharacterListView::getName, context.language());
    }

    private List<CharacterListView> list(
            String name, ToolContext context, int size, List<CharacterSortableField> sort) {
        CharacterFilter filter = new CharacterFilter().languageCode(context.language().languageCode());
        if (name != null && !name.isBlank()) {
            filter.name(name);
        }
        ListCharacterResponse response = characterService.listCharacters(new ListCharacterRequest()
                .pageSize(size)
                .pageIndex(1)
                .sortKeys(new ArrayList<>(sort))
                .characterFilter(filter));
        return response == null ? List.of() : nonNull(response.getCharacters());
    }

    private static List<String> hakis(List<Haki> hakis) {
        return nonNull(hakis).stream().map(Haki::getValue).toList();
    }
}
