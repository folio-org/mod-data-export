package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.exception.export.DataExportException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import static org.folio.dataexp.util.S3FilePathUtils.getPathToStoredFiles;

@Component
@RequiredArgsConstructor
@Log4j2
public class SlicerProcessor {

  private static final String CALL_SLICE_INSTANCES_IDS_PROCEDURE = "call slice_instances_ids(?, ?, ?)";
  private static final String FROM_TO_UUID_PART = "_%s_%s";
  private static final String MARC_EXTENSION = ".mrc";
  public static final int DEFAULT_SLICE_SIZE = 100_000;
  public static final String SLICE_SIZE_KEY = "slice_size";

  private final JdbcTemplate jdbcTemplate;
  private final ConfigurationService configurationService;

  public void sliceInstancesIds(FileDefinition fileDefinition) {
    var sliceSize = configurationService.getValue(SLICE_SIZE_KEY);
    sliceInstancesIds(fileDefinition, Integer.parseInt(sliceSize));
  }

  public void sliceInstancesIds(FileDefinition fileDefinition, int sliceSize) {
    var fileName = createFileNameWithPlaceHolder(fileDefinition.getFileName());
    var pathLocation = getPathToStoredFiles(fileDefinition.getJobExecutionId().toString(), fileName);
    try (Connection connection = jdbcTemplate.getDataSource().getConnection();
         CallableStatement callableStatement = connection.prepareCall(CALL_SLICE_INSTANCES_IDS_PROCEDURE)) {
      callableStatement.setString(1, fileDefinition.getJobExecutionId().toString());
      callableStatement.setString(2, pathLocation);
      callableStatement.setInt(3, sliceSize);
      callableStatement.executeUpdate();
    } catch (SQLException sqlException) {
      log.error("Exception for slice_instances_ids procedure call for fileDefinitionId {} with message {}",
        fileDefinition.getId(), sqlException.getMessage());
      throw new DataExportException(sqlException.getMessage());
    }
  }

  private String createFileNameWithPlaceHolder(String fileName) {
    var baseName = FilenameUtils.getBaseName(fileName);
    return baseName + FROM_TO_UUID_PART + MARC_EXTENSION;
  }
}
