package org.folio.util;

import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.cql.CQLWrapper;

public class HelperUtils {

  private static final String JOB_EXECUTION_ID_FIELD = "'jobExecutionId'";
  private static final String REASON_FIELD = "'reason'";

  private HelperUtils() {

  }

  /**
   * Builds criteria by which db result is filtered
   *
   * @param jsonbField - json key name
   * @param value      - value corresponding to the key
   * @return - Criteria object
   */
  public static Criteria constructCriteria(String jsonbField, String value) {
    Criteria criteria = new Criteria();
    criteria.addField(jsonbField);
    criteria.setOperation("=");
    criteria.setVal(value);
    return criteria;
  }


  /**
   * Builds CQLWrapper by which db result is filtered
   *
   * @param tableName - json key name
   * @param query     - query string to filter jobExecutions based on matching criteria in fields
   * @param limit     - limit of records for pagination
   * @param offset    - starting index in a list of results
   * @return - CQLWrapper
   */
  public static CQLWrapper getCQLWrapper(String tableName, String query, int limit, int offset) throws FieldException {
    CQL2PgJSON cql2pgJson = new CQL2PgJSON(tableName + ".jsonb");
    return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit))
      .setOffset(new Offset(offset));
  }

  /**
   * Builds Criterion by which db result is filtered to get error logs by job execution id and reason
   *
   * @param jobExecutionId - job execution id to which error log related
   * @param reason         - query string to filter error logs based on matching reason in fields
   * @return - {@link Criterion}}
   */
  public static Criterion getErrorLogCriterionByJobExecutionIdAndReason(String jobExecutionId, String reason) {
    Criterion criterion = new Criterion();
    Criteria jobExecutionIdCriteria = new Criteria();
    jobExecutionIdCriteria.addField(JOB_EXECUTION_ID_FIELD)
      .setOperation("=")
      .setVal(jobExecutionId);
    Criteria reasonCriteria = new Criteria();
    reasonCriteria.addField(REASON_FIELD)
      .setOperation("LIKE")
      .setVal("%" + reason + "%");
    criterion.addCriterion(jobExecutionIdCriteria);
    criterion.addCriterion(reasonCriteria);
    return criterion;
  }

}
