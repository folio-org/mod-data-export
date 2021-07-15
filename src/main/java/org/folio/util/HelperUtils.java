package org.folio.util;

import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.cql.CQLWrapper;

import java.util.List;

public class HelperUtils {

  private static final String JOB_EXECUTION_ID_FIELD = "'jobExecutionId'";
  private static final String ERROR_MESSAGE_CODE_FIELD = "'errorMessageCode'";

  private HelperUtils() {

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
   * @param errorMessageCode - query string to filter error logs based on matching reason in fields
   * @return - {@link Criterion}}
   */
  public static Criterion getErrorLogCriterionByJobExecutionIdAndErrorMessageCode(String jobExecutionId, String errorMessageCode) {
    Criterion criterion = new Criterion();
    Criteria jobExecutionIdCriteria = new Criteria();
    jobExecutionIdCriteria.addField(JOB_EXECUTION_ID_FIELD)
      .setOperation("=")
      .setVal(jobExecutionId);
    Criteria reasonCriteria = new Criteria();
    reasonCriteria.addField(ERROR_MESSAGE_CODE_FIELD)
      .setOperation("=")
      .setVal(errorMessageCode);
    criterion.addCriterion(jobExecutionIdCriteria);
    criterion.addCriterion(reasonCriteria);
    return criterion;
  }

  public static Criterion getErrorLogCriterionByJobExecutionIdAndErrorCodes(
      String jobExecutionId, List<String> errorCodes) {
    Criterion criterion = new Criterion();
    Criteria jobExecutionIdCriteria = new Criteria();
    jobExecutionIdCriteria
        .addField(JOB_EXECUTION_ID_FIELD)
        .setOperation("=")
        .setVal(jobExecutionId);
    Criteria reasonCriteria = new Criteria();
    reasonCriteria
        .addField(ERROR_MESSAGE_CODE_FIELD)
        .setOperation("SIMILAR TO")
        .setVal(
            errorCodes.size() > 1
                ? "%(" + String.join("|", errorCodes) + ")%"
                : "%" + errorCodes.get(0) + "%");

    criterion.addCriterion(jobExecutionIdCriteria);
    criterion.addCriterion(reasonCriteria);
    return criterion;
  }
}
