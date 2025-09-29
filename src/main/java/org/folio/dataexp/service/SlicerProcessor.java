package org.folio.dataexp.service;

import static org.folio.dataexp.util.S3FilePathUtils.getPathToStoredFiles;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.exception.export.DataExportException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Processor for slicing export files into batches for export operations.
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class SlicerProcessor {

  private static final String CALL_SLICE_INSTANCES_IDS_PROCEDURE =
      "call slice_instances_ids(?, ?, ?)";
  private static final String CALL_SLICE_INSTANCES_ALL_IDS_PROCEDURE =
      "call slice_instances_all_ids(?, ?, ?)";
  private static final String CALL_SLICE_HOLDINGS_ALL_IDS_PROCEDURE =
      "call slice_holdings_all_ids(?, ?, ?)";
  private static final String CALL_SLICE_AUTHORITIES_ALL_IDS_PROCEDURE =
      "call slice_authorities_all_ids(?, ?, ?)";
  private static final String FROM_TO_UUID_PART = "_%s_%s";
  private static final String MARC_EXTENSION = ".mrc";
  public static final int DEFAULT_SLICE_SIZE = 100_000;
  public static final String SLICE_SIZE_KEY = "slice_size";

  private final JdbcTemplate jdbcTemplate;
  private final ConfigurationService configurationService;

  /**
   * Slices instance IDs for export using the configured slice size.
   *
   * @param fileDefinition The file definition.
   * @param exportRequest The export request.
   */
  public void sliceInstancesIds(FileDefinition fileDefinition, ExportRequest exportRequest) {
    var sliceSize = configurationService.getValue(SLICE_SIZE_KEY);
    sliceInstancesIds(fileDefinition, Integer.parseInt(sliceSize), exportRequest);
  }

  /**
   * Slices instance IDs for export using a specified slice size.
   *
   * @param fileDefinition The file definition.
   * @param sliceSize The slice size.
   * @param exportRequest The export request.
   */
  public void sliceInstancesIds(FileDefinition fileDefinition, int sliceSize,
      ExportRequest exportRequest) {
    var fileName = createFileNameWithPlaceHolder(fileDefinition.getFileName());
    var pathLocation = getPathToStoredFiles(
        fileDefinition.getJobExecutionId().toString(),
        fileName
    );
    try (
        Connection connection = jdbcTemplate.getDataSource().getConnection();
        CallableStatement callableStatement = connection.prepareCall(
            selectProcedure(exportRequest)
        )
    ) {
      callableStatement.setString(1, fileDefinition.getJobExecutionId().toString());
      callableStatement.setString(2, pathLocation);
      callableStatement.setInt(3, sliceSize);
      callableStatement.executeUpdate();
    } catch (SQLException sqlException) {
      log.error(
          "Exception for slice_instances_ids procedure call for"
          + " fileDefinitionId {} with message {}",
          fileDefinition.getId(),
          sqlException.getMessage()
      );
      throw new DataExportException(sqlException.getMessage());
    }
  }

  /**
   * Creates a file name with placeholders for slicing.
   *
   * @param fileName The base file name.
   * @return The file name with placeholders.
   */
  private String createFileNameWithPlaceHolder(String fileName) {
    var baseName = FilenameUtils.getBaseName(fileName);
    return baseName + FROM_TO_UUID_PART + MARC_EXTENSION;
  }

  /**
   * Selects the appropriate stored procedure for slicing based on the export request.
   *
   * @param exportRequest The export request.
   * @return The stored procedure name.
   */
  private String selectProcedure(ExportRequest exportRequest) {
    if (Boolean.TRUE.equals(exportRequest.getAll())) {
      if (exportRequest.getIdType() == ExportRequest.IdTypeEnum.INSTANCE) {
        return CALL_SLICE_INSTANCES_ALL_IDS_PROCEDURE;
      } else if (exportRequest.getIdType() == ExportRequest.IdTypeEnum.HOLDING) {
        return CALL_SLICE_HOLDINGS_ALL_IDS_PROCEDURE;
      }
      return CALL_SLICE_AUTHORITIES_ALL_IDS_PROCEDURE;
    }
    return CALL_SLICE_INSTANCES_IDS_PROCEDURE;
  }
}
