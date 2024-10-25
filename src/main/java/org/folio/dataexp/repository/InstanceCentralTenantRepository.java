package org.folio.dataexp.repository;

import static java.lang.String.format;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class InstanceCentralTenantRepository {
  @PersistenceContext
  private EntityManager entityManager;

  private final static String INSTANCE_QUERY = "SELECT id, jsonb FROM %s_mod_inventory_storage.instance WHERE id in :ids";

  public List<InstanceEntity> findInstancesByIdIn(String tenantId, Set<UUID> ids) {
    String sql = format(INSTANCE_QUERY, tenantId, tenantId);
    Query q = entityManager.createNativeQuery(sql, InstanceEntity.class);
    q.setParameter("ids", ids);
    return q.getResultList();
  }

}
