package org.folio.dataexp.repository;

import java.util.List;
import java.util.UUID;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository for {@link JobProfileEntity}.
 */
public interface JobProfileEntityCqlRepository
    extends JpaCqlRepository<JobProfileEntity, UUID> {

  String FIND_JOB_PROFILES_DATA_FROM_JOB_EXECUTIONS =
      "SELECT DISTINCT jobprofileid, jsonb ->> 'jobProfileName' FROM job_executions"
      + " OFFSET ?1 LIMIT ?2";

  /**
   * Gets used job profiles data from job executions.
   *
   * @param offset offset value
   * @param limit limit value
   * @return list of job profile data arrays
   */
  @Query(
      value = FIND_JOB_PROFILES_DATA_FROM_JOB_EXECUTIONS,
      nativeQuery = true
  )
  List<Object[]> getUsedJobProfilesData(Integer offset, Integer limit);
}
