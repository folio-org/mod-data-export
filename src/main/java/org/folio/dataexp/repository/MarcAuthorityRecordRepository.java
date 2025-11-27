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

/** Tenant-specific repository for {@link MarcRecordEntity} authority records. */
@Repository
public class MarcAuthorityRecordRepository {

  @PersistenceContext private EntityManager entityManager;

  private static final String QUERY_NON_DELETED =
      "SELECT id, content, external_id, record_type, state, leader_record_status,"
          + " suppress_discovery, generation"
          + " FROM %s_mod_source_record_storage.records_lb records_lb"
          + " JOIN %s_mod_source_record_storage.marc_records_lb using(id)"
          + " WHERE state IN ('ACTUAL', 'DELETED')"
          + " AND record_type = 'MARC_AUTHORITY'"
          + " AND external_id in :ids";

  private static final String QUERY_INCLUDING_DELETED =
      "SELECT id, content, external_id, record_type, state, leader_record_status,"
          + " suppress_discovery, generation"
          + " FROM %s_mod_source_record_storage.records_lb records_lb"
          + " JOIN %s_mod_source_record_storage.marc_records_lb using(id)"
          + " WHERE record_type = 'MARC_AUTHORITY'"
          + " AND external_id in :ids";

  /**
   * Finds non-deleted authority records by external IDs for a tenant.
   *
   * @param tenantId tenant ID
   * @param ids set of external UUIDs
   * @return list of MarcRecordEntity
   */
  public List<MarcRecordEntity> findNonDeletedByExternalIdIn(String tenantId, Set<UUID> ids) {
    String sql = format(QUERY_NON_DELETED, tenantId, tenantId);
    Query q = entityManager.createNativeQuery(sql, MarcRecordEntity.class);
    q.setParameter("ids", ids);
    return q.getResultList();
  }

  /**
   * Finds all authority records by external IDs for a tenant.
   *
   * @param tenantId tenant ID
   * @param ids set of external UUIDs
   * @return list of MarcRecordEntity
   */
  public List<MarcRecordEntity> findAllByExternalIdIn(String tenantId, Set<UUID> ids) {
    String sql = format(QUERY_INCLUDING_DELETED, tenantId, tenantId);
    Query q = entityManager.createNativeQuery(sql, MarcRecordEntity.class);
    q.setParameter("ids", ids);
    return q.getResultList();
  }
}
