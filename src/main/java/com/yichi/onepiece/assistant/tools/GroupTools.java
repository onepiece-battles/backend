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
import com.yichi.onepiece.group.service.GroupService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.CharacterInGroupView;
import com.yichi.onepiece.model.GetGroupRequest;
import com.yichi.onepiece.model.Group;
import com.yichi.onepiece.model.GroupFilter;
import com.yichi.onepiece.model.GroupFullView;
import com.yichi.onepiece.model.GroupListView;
import com.yichi.onepiece.model.ListChildCharactersRequest;
import com.yichi.onepiece.model.ListChildCharactersResponse;
import com.yichi.onepiece.model.ListChildGroupsRequest;
import com.yichi.onepiece.model.ListChildGroupsResponse;
import com.yichi.onepiece.model.ListGroupRequest;
import com.yichi.onepiece.model.ListGroupResponse;
import com.yichi.onepiece.model.Permission;
import com.yichi.onepiece.model.ResourceType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Crew / group lookups: by name, and one crew's members, child groups and parent. */
@Component
public class GroupTools implements AssistantToolProvider {
    private static final Logger logger = LoggerFactory.getLogger(GroupTools.class);
    static final int SEARCH_LIMIT = 8;

    private final GroupService groupService;
    private final IamService iamService;

    public GroupTools(GroupService groupService, IamService iamService) {
        this.groupService = groupService;
        this.iamService = iamService;
    }

    @Override
    public List<AssistantTool> tools() {
        String byName = """
                {"type": "object", "properties": {"name": {"type": "string"}}, "required": ["name"]}""";
        return List.of(
                new AssistantTool("search_groups",
                        "Search crews / groups by (partial) name.",
                        ToolJson.schema(byName),
                        (arguments, context) -> searchGroups(text(arguments, "name"), context),
                        (arguments, language) -> language.pick(
                                "正在搜索团体「" + text(arguments, "name") + "」",
                                "Searching crews \"" + text(arguments, "name") + "\"")),
                new AssistantTool("get_group_detail",
                        "A crew / group by name: its members (characters + their position), its child "
                                + "groups and its parent group.",
                        ToolJson.schema(byName),
                        (arguments, context) -> groupDetail(text(arguments, "name"), context),
                        (arguments, language) -> language.pick(
                                "正在查询团体「" + text(arguments, "name") + "」的成员",
                                "Looking up the crew " + text(arguments, "name"))));
    }

    ToolResult searchGroups(String name, ToolContext context) {
        check(ResourceType.GROUP, Permission.LIST);
        AssistantLanguage language = context.language();
        List<ResourceLink> links = new ArrayList<>();
        List<Map<String, Object>> groups = new ArrayList<>();
        for (GroupListView group : list(name, context, SEARCH_LIMIT)) {
            String groupName = language.displayName(group.getName());
            links.add(link("group", group.getId(), groupName));
            groups.add(map("id", group.getId(), "name", groupName));
        }
        return result(map("groups", groups), links);
    }

    ToolResult groupDetail(String name, ToolContext context) {
        check(ResourceType.GROUP, Permission.LIST);
        List<GroupListView> found = list(name, context, 30);
        if (found.isEmpty()) {
            return result(map("found", false, "query", name), List.of());
        }
        AssistantLanguage language = context.language();
        GroupListView group = singleMatch(found, name, "group", GroupListView::getId, GroupListView::getName, language).orElseThrow();
        String groupName = language.displayName(group.getName());
        List<ResourceLink> links = new ArrayList<>();
        links.add(link("group", group.getId(), groupName));

        // Member and child-group lookups use the same permissions as their endpoints.
        check(ResourceType.GROUP, Permission.GET);
        check(ResourceType.CHARACTER, Permission.LIST);
        ListChildCharactersResponse membersResponse =
                groupService.listChildCharacters(new ListChildCharactersRequest().groupId(group.getId()));
        List<Map<String, Object>> members = new ArrayList<>();
        for (CharacterInGroupView member : membersResponse == null ? List.<CharacterInGroupView>of()
                : nonNull(membersResponse.getChildCharacters())) {
            String memberName = language.displayName(member.getName());
            if (member.getCharacterId() != null) {
                links.add(link("character", member.getCharacterId(), memberName));
            }
            members.add(map("id", member.getCharacterId(), "name", memberName,
                    "position", member.getPosition() == null ? null : member.getPosition().getValue()));
        }

        check(ResourceType.CHARACTER, Permission.GET);
        ListChildGroupsResponse childResponse =
                groupService.listChildGroups(new ListChildGroupsRequest().groupId(group.getId()));
        List<Map<String, Object>> childGroups = new ArrayList<>();
        for (Group child : childResponse == null ? List.<Group>of() : nonNull(childResponse.getChildGroups())) {
            String childName = language.displayName(child.getName());
            links.add(link("group", child.getId(), childName));
            childGroups.add(map("id", child.getId(), "name", childName));
        }

        Map<String, Object> parent = null;
        if (group.getFatherGroupId() != null) {
            try {
                GroupFullView parentView = groupService.getGroupFullView(new GetGroupRequest().id(group.getFatherGroupId()));
                if (parentView != null) {
                    String parentName = language.displayName(parentView.getName());
                    links.add(link("group", parentView.getId(), parentName));
                    parent = map("id", parentView.getId(), "name", parentName);
                }
            } catch (RuntimeException e) {
                // The crew itself is still worth returning without its parent.
                logger.debug("could not load parent group {} for the assistant", group.getFatherGroupId(), e);
            }
        }

        return result(map(
                "id", group.getId(),
                "name", groupName,
                "members", members,
                "child_groups", childGroups,
                "parent_group", parent), links);
    }

    private void check(ResourceType type, Permission permission) {
        iamService.checkPermissionOfCurrentAccount(type, Set.of(permission));
    }

    private List<GroupListView> list(String name, ToolContext context, int size) {
        ListGroupResponse response = groupService.list(new ListGroupRequest()
                .pageSize(size)
                .pageIndex(1)
                .sortKeys(new ArrayList<>())
                .groupFilter(new GroupFilter().name(name).languageCode(context.language().languageCode()).userId(context.userId()).systemResource(true)));
        return response == null ? List.of() : nonNull(response.getGroups());
    }
}
