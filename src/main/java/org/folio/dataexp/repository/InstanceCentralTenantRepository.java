package org.folio.dataexp.repository;

import static java.lang.String.format;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.springframework.stereotype.Repository;

/** Tenant-specific repository for {@link InstanceEntity}. */
@Repository
public class InstanceCentralTenantRepository {

  @PersistenceContext private EntityManager entityManager;

  private static final String INSTANCE_QUERY =
      "SELECT id, jsonb FROM %s_mod_inventory_storage.instance WHERE id in :ids";

  /**
   * Finds instances by a set of IDs for a specific tenant.
   *
   * @param tenantId tenant ID
   * @param ids set of UUIDs
   * @return list of instance entities
   */
  public List<InstanceEntity> findInstancesByIdIn(String tenantId, Set<UUID> ids) {
    String sql = format(INSTANCE_QUERY, tenantId, tenantId);
    Query q = entityManager.createNativeQuery(sql, InstanceEntity.class);
    q.setParameter("ids", ids);
    return q.getResultList();
  }
}
