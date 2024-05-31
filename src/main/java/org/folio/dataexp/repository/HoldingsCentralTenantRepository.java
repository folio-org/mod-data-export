package org.folio.dataexp.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.lang.String.format;

@Repository
public class HoldingsCentralTenantRepository {

  @PersistenceContext
  private EntityManager entityManager;

  private final static String HOLDINGS_QUERY = "SELECT id, jsonb FROM %s_mod_inventory_storage.holdings_record WHERE id in :ids";

  public List<HoldingsRecordEntity> findHoldingsByIdIn(String tenantId, Set<UUID> ids) {
    String sql = format(HOLDINGS_QUERY, tenantId, tenantId);
    Query q = entityManager.createNativeQuery(sql, HoldingsRecordEntity.class);
    q.setParameter("ids", ids);
    return q.getResultList();
  }
}
