package com.yichi.onepiece.configuration;

import com.yichi.onepiece.iam.PermissionDefinition;
import com.yichi.onepiece.iam.model.ResourceDao;
import com.yichi.onepiece.iam.model.ResourceEntity;
import com.yichi.onepiece.iam.model.ResourceEntityBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class LoadResourceAndPermissionData implements CommandLineRunner {
  private final ResourceDao resourceDao;

  public LoadResourceAndPermissionData(ResourceDao resourceDao) {
    this.resourceDao = resourceDao;
  }

  @Override
  public void run(String... args) throws Exception {
    List<ResourceEntity> resourceEntities = new ArrayList<>();
    var resourcePermissionsMap = PermissionDefinition.getResourcePermissionMap();
    for (Map.Entry<String, Set<String>> entry : resourcePermissionsMap.entrySet()) {
      var resource =
          ResourceEntityBuilder.newBuilder()
              .setName(entry.getKey())
              .setPermissions(entry.getValue())
              .build();
      resourceEntities.add(resource);
    }
    resourceDao.batchInsert(resourceEntities);
  }
}
