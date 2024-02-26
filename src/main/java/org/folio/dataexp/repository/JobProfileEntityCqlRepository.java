package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface JobProfileEntityCqlRepository extends JpaCqlRepository<JobProfileEntity, UUID> {
  String FIND_JOB_PROFILES_DATA_FROM_JOB_EXECUTIONS =
    "SELECT DISTINCT job_profile_id, jsonb ->> 'jobProfileName' FROM job_executions OFFSET ?1 LIMIT ?2";

  @Query(value = FIND_JOB_PROFILES_DATA_FROM_JOB_EXECUTIONS, nativeQuery = true)
  List<Object[]> getUsedJobProfilesData(Integer offset, Integer limit);

}
