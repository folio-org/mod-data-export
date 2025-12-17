package org.folio.dataexp.repository;

import static java.lang.String.format;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.springframework.stereotype.Repository;

/** Tenant-specific repository for {@link MarcRecordEntity} MARC instance records. */
@Repository
public class MarcInstanceRecordRepository {

  @PersistenceContext private EntityManager entityManager;

  private static final String QUERY_ACTUAL_NON_DELETED =
      "SELECT id, content, external_id, record_type, leader_record_status,"
          + " suppress_discovery, state, generation"
          + " FROM %s_mod_source_record_storage.records_lb records_lb"
          + " JOIN %s_mod_source_record_storage.marc_records_lb using(id)"
          + " WHERE records_lb.state = 'ACTUAL'"
          + " AND records_lb.leader_record_status != 'd'"
          + " AND records_lb.record_type = 'MARC_BIB'"
          + " AND external_id in :ids";

  private static final String QUERY_ACTUAL_AND_DELETED =
      "SELECT id, content, external_id, record_type, leader_record_status,"
          + " suppress_discovery, state, generation"
          + " FROM %s_mod_source_record_storage.records_lb records_lb"
          + " JOIN %s_mod_source_record_storage.marc_records_lb using(id)"
          + " WHERE records_lb.state in ('ACTUAL', 'DELETED')"
          + " AND records_lb.record_type = 'MARC_BIB'"
          + " AND external_id in :ids";

  /**
   * Finds actual and non-deleted MARC instance records by external IDs for a tenant.
   *
   * @param tenantId tenant ID
   * @param ids set of external UUIDs
   * @return list of MarcRecordEntity
   */
  public List<MarcRecordEntity> findByExternalIdIn(String tenantId, Set<UUID> ids) {
    String sql = format(QUERY_ACTUAL_NON_DELETED, tenantId, tenantId);
    Query q = entityManager.createNativeQuery(sql, MarcRecordEntity.class);
    q.setParameter("ids", ids);
    return q.getResultList();
  }

  /**
   * Finds actual or deleted MARC instance records by external IDs for a tenant.
   *
   * @param tenantId tenant ID
   * @param ids set of external UUIDs
   * @return list of MarcRecordEntity
   */
  public List<MarcRecordEntity> findActualAndDeletedByExternalIdIn(String tenantId, Set<UUID> ids) {
    String sql = format(QUERY_ACTUAL_AND_DELETED, tenantId, tenantId);
    Query q = entityManager.createNativeQuery(sql, MarcRecordEntity.class);
    q.setParameter("ids", ids);
    return q.getResultList();
  }
}
