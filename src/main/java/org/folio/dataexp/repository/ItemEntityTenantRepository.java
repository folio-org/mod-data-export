package org.folio.dataexp.repository;

import static java.lang.String.format;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.domain.entity.ItemEntity;
import org.springframework.stereotype.Repository;

/** Tenant-specific repository for {@link ItemEntity}. */
@Repository
public class ItemEntityTenantRepository {

  private static final String ITEMS_QUERY =
      "SELECT id, jsonb, holdings_record_id FROM %s_mod_data_export.v_item"
          + " WHERE holdings_record_id in :ids";

  @PersistenceContext private EntityManager entityManager;

  /**
   * Finds items by a set of holdings record IDs for a specific tenant.
   *
   * @param tenantId tenant ID
   * @param ids set of holdings record UUIDs
   * @return list of item entities
   */
  public List<ItemEntity> findByHoldingsRecordIdIn(String tenantId, Set<UUID> ids) {
    String sql = format(ITEMS_QUERY, tenantId, tenantId);
    Query q = entityManager.createNativeQuery(sql, ItemEntity.class);
    q.setParameter("ids", ids);
    return q.getResultList();
  }
}
