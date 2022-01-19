package org.folio.service.loader;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.folio.service.manager.export.strategy.AbstractExportStrategy;

public class LoadResult {

  private static final AbstractExportStrategy.EntityType DEFAULT_ENTITY_TYPE = AbstractExportStrategy.EntityType.INSTANCE;

  private List<JsonObject> entities = new ArrayList<>();
  private Collection<String> notFoundEntitiesUUIDs = new ArrayList<>();
  private AbstractExportStrategy.EntityType entityType;

  public List<JsonObject> getEntities() {
    return entities;
  }

  public void setEntities(List<JsonObject> entities) {
    this.entities = entities;
  }

  public Collection<String> getNotFoundEntitiesUUIDs() {
    return notFoundEntitiesUUIDs;
  }

  public void setNotFoundEntitiesUUIDs(Collection<String> notFoundEntitiesUUIDs) {
    this.notFoundEntitiesUUIDs = notFoundEntitiesUUIDs;
  }

  public AbstractExportStrategy.EntityType getEntityType() {
    if (Objects.isNull(entityType)) {
      return DEFAULT_ENTITY_TYPE;
    }
    return entityType;
  }

  public void setEntityType(AbstractExportStrategy.EntityType entityType) {
    this.entityType = entityType;
  }

}
