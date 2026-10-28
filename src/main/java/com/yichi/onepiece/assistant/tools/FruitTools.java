package com.yichi.onepiece.assistant.tools;

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
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.CharacterInFruitView;
import com.yichi.onepiece.model.FruitFilter;
import com.yichi.onepiece.model.FruitListView;
import com.yichi.onepiece.model.FruitType;
import com.yichi.onepiece.model.ListFruitRequest;
import com.yichi.onepiece.model.ListFruitResponse;
import com.yichi.onepiece.model.Permission;
import com.yichi.onepiece.model.ResourceType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Devil-fruit lookups: by name, their holders, and every fruit of a type. */
@Component
public class FruitTools implements AssistantToolProvider {
    static final int SEARCH_LIMIT = 6;
    static final int TYPE_LIMIT = 30;

    private final FruitService fruitService;
    private final IamService iamService;

    public FruitTools(FruitService fruitService, IamService iamService) {
        this.fruitService = fruitService;
        this.iamService = iamService;
    }

    @Override
    public List<AssistantTool> tools() {
        String byName = """
                {"type": "object", "properties": {"name": {"type": "string"}}, "required": ["name"]}""";
        return List.of(
                new AssistantTool("search_fruits",
                        "Search devil fruits by (partial) name. Returns id, name, type and current holders.",
                        ToolJson.schema(byName),
                        (arguments, context) -> searchFruits(text(arguments, "name"), context),
                        (arguments, language) -> fruitLabel(text(arguments, "name"), language)),
                new AssistantTool("get_fruit_holders",
                        "Who ate / currently holds a devil fruit, by fruit name.",
                        ToolJson.schema(byName),
                        (arguments, context) -> searchFruits(text(arguments, "name"), context),
                        (arguments, language) -> fruitLabel(text(arguments, "name"), language)),
                new AssistantTool("list_fruits_by_type",
                        "List devil fruits of a given type and their holders. Use this for questions like "
                                + "\"all Ancient Zoan users\".",
                        ToolJson.schema("""
                                {"type": "object",
                                 "properties": {"type": {"type": "string",
                                   "description": "One of: Paramecia, Logia, Zoan, Ancient_Zoan, Mythical_Zoan, Artificial_Zoan"}},
                                 "required": ["type"]}"""),
                        (arguments, context) -> fruitsByType(text(arguments, "type"), context),
                        (arguments, language) -> language.pick(
                                "正在列出「" + text(arguments, "type") + "」类恶魔果实",
                                "Listing " + text(arguments, "type") + " devil fruits")));
    }

    ToolResult searchFruits(String name, ToolContext context) {
        checkList();
        FruitFilter filter = new FruitFilter().name(name).languageCode(context.language().languageCode()).userId(context.userId()).systemResource(true);
        List<ResourceLink> links = new ArrayList<>();
        List<Map<String, Object>> fruits = describe(list(filter, SEARCH_LIMIT), context.language(), links);
        return result(map("fruits", fruits), links);
    }

    ToolResult fruitsByType(String rawType, ToolContext context) {
        FruitType type = parseFruitType(rawType);
        if (type == null) {
            return result(map("error", "unknown fruit type", "query", rawType), List.of());
        }
        checkList();
        FruitFilter filter = new FruitFilter()
                .types(new ArrayList<>(List.of(type)))
                .languageCode(context.language().languageCode());
        List<ResourceLink> links = new ArrayList<>();
        List<Map<String, Object>> fruits = describe(list(filter, TYPE_LIMIT), context.language(), links);
        return result(map("type", type.getValue(), "count", fruits.size(), "fruits", fruits), links);
    }

    /** Accepts the API names plus the common Chinese names the model may pass through. */
    static FruitType parseFruitType(String raw) {
        String type = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        return switch (type) {
            case "paramecia", "超人系" -> FruitType.PARAMECIA;
            case "logia", "自然系" -> FruitType.LOGIA;
            case "zoan", "动物系" -> FruitType.ZOAN;
            case "ancient_zoan", "ancientzoan", "古代种" -> FruitType.ANCIENT_ZOAN;
            case "mythical_zoan", "mythicalzoan", "幻兽种", "神话种" -> FruitType.MYTHICAL_ZOAN;
            case "artificial_zoan", "artificialzoan", "人造", "人造恶魔果实" -> FruitType.ARTIFICIAL_ZOAN;
            default -> null;
        };
    }

    private List<Map<String, Object>> describe(
            List<FruitListView> fruits, AssistantLanguage language, List<ResourceLink> links) {
        List<Map<String, Object>> described = new ArrayList<>();
        for (FruitListView fruit : fruits) {
            String fruitName = language.displayName(fruit.getName());
            links.add(link("fruit", fruit.getId(), fruitName));
            List<Map<String, Object>> holders = new ArrayList<>();
            for (CharacterInFruitView holder : nonNull(fruit.getCharacters())) {
                String holderName = language.displayName(holder.getName());
                links.add(link("character", holder.getId(), holderName));
                holders.add(map("id", holder.getId(), "name", holderName));
            }
            described.add(map("id", fruit.getId(), "name", fruitName,
                    "type", fruit.getFruitType() == null ? null : fruit.getFruitType().getValue(),
                    "holders", holders));
        }
        return described;
    }

    private void checkList() {
        iamService.checkPermissionOfCurrentAccount(ResourceType.FRUIT, Set.of(Permission.LIST));
    }

    private List<FruitListView> list(FruitFilter filter, int size) {
        ListFruitResponse response = fruitService.listFruit(new ListFruitRequest()
                .pageSize(size)
                .pageIndex(1)
                .sortKeys(new ArrayList<>())
                .fruitFilter(filter));
        return response == null ? List.of() : nonNull(response.getFruits());
    }

    private static String fruitLabel(String name, AssistantLanguage language) {
        return language.pick("正在查询恶魔果实「" + name + "」", "Looking up the devil fruit " + name);
    }
}
