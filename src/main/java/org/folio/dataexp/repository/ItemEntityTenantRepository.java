package org.folio.dataexp.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.folio.dataexp.domain.entity.ItemEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.lang.String.format;

public class ItemEntityTenantRepository {

  private final static String ITEMS_QUERY = "SELECT id, jsonb, holdings_record_id FROM %s_mod_data_export.v_item WHERE holdings_record_id in :ids";

  @PersistenceContext
  private EntityManager entityManager;

  public List<ItemEntity> findByHoldingsRecordIdIn(String tenantId, Set<UUID> ids) {
    String sql = format(ITEMS_QUERY, tenantId, tenantId);
    Query q = entityManager.createNativeQuery(sql, ItemEntity.class);
    q.setParameter("ids", ids);
    return q.getResultList();
  }
}
