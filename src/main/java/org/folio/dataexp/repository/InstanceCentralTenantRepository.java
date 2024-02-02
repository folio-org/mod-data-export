package org.folio.dataexp.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.lang.String.format;

@Repository
public class InstanceCentralTenantRepository {
  @PersistenceContext
  private EntityManager entityManager;

  private final static String MARC_RECORD_QUERY = "SELECT id, content, external_id, record_type FROM %s_mod_source_record_storage.records_lb records_lb\n"
      + "    JOIN %s_mod_source_record_storage.marc_records_lb using(id)\n"
      + "    WHERE records_lb.state = 'ACTUAL' AND records_lb.leader_record_status != 'd' AND records_lb.record_type = 'MARC_BIB'"
      + "    AND external_id in :ids";

  private final static String INSTANCE_QUERY = "SELECT id, jsonb FROM %s_mod_inventory_storage.instance WHERE id in :ids";

  public List<MarcRecordEntity> findMarcRecordsByExternalIdIn(String tenantId, Set<UUID> ids) {
    String sql = format(MARC_RECORD_QUERY, tenantId, tenantId);
    Query q = entityManager.createNativeQuery(sql, MarcRecordEntity.class);
    q.setParameter("ids", ids);
    return q.getResultList();
  }

  public List<InstanceEntity> findInstancesByIdIn(String tenantId, Set<UUID> ids) {
    String sql = format(INSTANCE_QUERY, tenantId, tenantId);
    Query q = entityManager.createNativeQuery(sql, InstanceEntity.class);
    q.setParameter("ids", ids);
    return q.getResultList();
  }

}
