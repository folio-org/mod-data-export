package org.folio.dataexp.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.lang.String.format;

@Repository
public class HoldingsRecordEntityTenantRepository {

  private final static String HOLDINGS_QUERY = "SELECT id, jsonb, instance_id FROM %s_mod_data_export.v_holdings_record WHERE id in :ids";

  @PersistenceContext
  private EntityManager entityManager;

  public List<HoldingsRecordEntity> findByIdIn(String tenantId, Set<UUID> ids) {
    String sql = format(HOLDINGS_QUERY, tenantId, tenantId);
    Query q = entityManager.createNativeQuery(sql, HoldingsRecordEntity.class);
    q.setParameter("ids", ids);
    return q.getResultList();
  }
}
