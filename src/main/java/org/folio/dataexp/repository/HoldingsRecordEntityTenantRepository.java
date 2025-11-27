package org.folio.dataexp.repository;

import static java.lang.String.format;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.springframework.stereotype.Repository;

/** Tenant-specific repository for {@link HoldingsRecordEntity}. */
@Repository
public class HoldingsRecordEntityTenantRepository {

  private static final String HOLDINGS_QUERY =
      "SELECT id, jsonb, instance_id FROM %s_mod_data_export.v_holdings_record WHERE id in :ids";

  @PersistenceContext private EntityManager entityManager;

  /**
   * Finds holdings by a set of IDs for a specific tenant.
   *
   * @param tenantId tenant ID
   * @param ids set of UUIDs
   * @return list of holdings record entities
   */
  public List<HoldingsRecordEntity> findByIdIn(String tenantId, Set<UUID> ids) {
    String sql = format(HOLDINGS_QUERY, tenantId, tenantId);
    Query q = entityManager.createNativeQuery(sql, HoldingsRecordEntity.class);
    q.setParameter("ids", ids);
    return q.getResultList();
  }
}
