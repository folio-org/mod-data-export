package org.folio.dataexp.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.lang.String.format;

@Repository
public class MarcAuthorityRecordRepository {

  @PersistenceContext
  private EntityManager entityManager;
  private final static String QUERY = "SELECT id, content, external_id, record_type, state, leader_record_status, suppress_discovery\n"
      + "    FROM %s_mod_source_record_storage.records_lb records_lb\n"
      + "    JOIN %s_mod_source_record_storage.marc_records_lb using(id)"
      + "    WHERE state = 'ACTUAL' AND leader_record_status != 'd' AND record_type = 'MARC_AUTHORITY'"
      + "    AND external_id in :ids";

  public List<MarcRecordEntity> findByExternalIdIn(String tenantId, Set<UUID> ids) {
    String sql = format(QUERY, tenantId, tenantId);
    Query q = entityManager.createNativeQuery(sql, MarcRecordEntity.class);
    q.setParameter("ids", ids);
    return q.getResultList();
  }
}
