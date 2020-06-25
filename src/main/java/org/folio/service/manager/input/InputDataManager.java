package org.folio.service.manager.input;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.Map;

import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.service.manager.export.ExportResult;

@ProxyGen
public interface InputDataManager {  //NOSONAR
  String INPUT_DATA_MANAGER_ADDRESS = "input-data-manager.queue";     //NOSONAR

  static InputDataManager create(Context context) {
    return new InputDataManagerImpl(context);
  }

  static InputDataManager createProxy(Vertx vertx) {
    return new InputDataManagerVertxEBProxy(vertx, INPUT_DATA_MANAGER_ADDRESS);
  }

  /**
   * Initialize and start an export process of uuids chunks are read from the file storage
   *
   * @param exportRequest         - entity that contains request parameters, such as
   *                              fileDefinition, jobProfile, etc.
   * @param requestFileDefinition - {@link FileDefinition}
   * @param mappingProfile        - {@link MappingProfile}
   * @param jobExecution          - {@link JobExecution}
   * @param params                - okapi headers and connection parameters
   */
  void init(JsonObject exportRequest, JsonObject requestFileDefinition, JsonObject mappingProfile, JsonObject jobExecution, Map<String, String> params);

  /**
   * Publish the next chunk of uuids to be exported. If there is no more uuids to read,
   * the export is considered completed.
   *
   * @param payload - payload object that contains a chunk identifiers for export and additional
   *                  fields such as filedefinition, okapi headers, jobexecutionid etc.
   * @param exportResult - result status of an export previous payload
   */
  void proceed(JsonObject payload, ExportResult exportResult);
}
