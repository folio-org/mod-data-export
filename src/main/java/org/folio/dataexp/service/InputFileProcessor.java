package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.cql2pgjson.exception.QueryValidationException;
import org.folio.cql2pgjson.exception.ServerChoiceIndexesException;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.exception.export.DataExportException;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.util.S3FilePathUtils;
import org.folio.s3.client.FolioS3Client;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Log4j2
public class InputFileProcessor {

  private static final String CALL_SAVE_INSTANCES_IDS_PROCEDURE = "call save_instances_ids(?, ?)";
  private static final int BATCH_SIZE_TO_SAVE = 1000;
  private final ExportIdEntityRepository exportIdEntityRepository;
  private final JdbcTemplate jdbcTemplate;
  private final FolioS3Client s3Client;

  public void readFile(FileDefinition fileDefinition, CommonExportFails commonExportFails) {
    try {
      if (fileDefinition.getUploadFormat() == FileDefinition.UploadFormatEnum.CQL) {
        readCqlFile(fileDefinition);
      } else {
        readCsvFile(fileDefinition, commonExportFails);
      }
    } catch (Exception e) {
      throw new DataExportException(e.getMessage());
    }
  }

  private void readCsvFile(FileDefinition fileDefinition, CommonExportFails commonExportFails) throws IOException {
    var pathToRead = S3FilePathUtils.getPathToUploadedFiles(fileDefinition.getId(), fileDefinition.getFileName());
    var batch = new ArrayList<ExportIdEntity>();
    var duplicatedIds = new HashSet<UUID>();
    try (InputStream is = s3Client.read(pathToRead); BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      reader.lines().forEach(id -> {
        var instanceId = id.replace("\"", StringUtils.EMPTY);
        try {
          var entity = ExportIdEntity.builder().jobExecutionId(fileDefinition
            .getJobExecutionId()).instanceId(UUID.fromString(instanceId)).build();
          if (!duplicatedIds.contains(entity.getInstanceId())) {
            batch.add(entity);
            duplicatedIds.add(entity.getInstanceId());
          } else {
            commonExportFails.incrementDuplicatedUUID();
          }
        } catch (Exception e) {
          log.error("Error converting {} to uuid", id);
          commonExportFails.addToInvalidUUIDFormat(id);
        }
        if (batch.size() == BATCH_SIZE_TO_SAVE) {
          var duplicatedFromDb = findDuplicatedUUIDFromDb(new HashSet<>(batch.stream().map(ExportIdEntity::getInstanceId).toList()), fileDefinition.getJobExecutionId());
          commonExportFails.incrementDuplicatedUUID(duplicatedFromDb.size());
          batch.removeIf(e -> duplicatedFromDb.contains(e.getInstanceId()));
          exportIdEntityRepository.saveAll(batch);
          batch.clear();
          duplicatedIds.clear();
        }
      });
    }
    var duplicatedFromDb = findDuplicatedUUIDFromDb(new HashSet<>(batch.stream().map(ExportIdEntity::getInstanceId).toList()), fileDefinition.getJobExecutionId());
    commonExportFails.incrementDuplicatedUUID(duplicatedFromDb.size());
    batch.removeIf(e -> duplicatedFromDb.contains(e.getInstanceId()));
    exportIdEntityRepository.saveAll(batch);
  }

  private List<UUID> findDuplicatedUUIDFromDb(Set<UUID> ids, UUID jobExecutionId) {
    return exportIdEntityRepository.findByInstanceIdInAndJobExecutionIdIs(ids, jobExecutionId).stream().map(ExportIdEntity::getInstanceId).toList();
  }

  private void readCqlFile(FileDefinition fileDefinition) throws IOException, ServerChoiceIndexesException, FieldException, QueryValidationException, SQLException {
    var pathToRead = S3FilePathUtils.getPathToUploadedFiles(fileDefinition.getId(), fileDefinition.getFileName());
    String cql;
    try (InputStream is = s3Client.read(pathToRead)) {
      cql = IOUtils.toString(is, StandardCharsets.UTF_8);
    }
    if (Objects.nonNull(cql)) {
      CQL2PgJSON converter = new CQL2PgJSON("jsonb", Collections.emptyList());
      var whereClause = converter.toSql(cql).toString();
      //ToDo f_unaccent was replaced as it was dropped
      whereClause = whereClause.replace("f_unaccent(", "unaccent(");
      try (Connection connection = jdbcTemplate.getDataSource().getConnection();
        CallableStatement callableStatement = connection.prepareCall(CALL_SAVE_INSTANCES_IDS_PROCEDURE)) {
        callableStatement.setString(1, fileDefinition.getJobExecutionId().toString());
        callableStatement.setString(2, whereClause);
        callableStatement.executeUpdate();
      } catch (SQLException sqlException) {
          log.error("Exception for save_instances_ids procedure call for fileDefinitionId {} with message {}",
            fileDefinition.getId(), sqlException.getMessage());
          throw sqlException;
      }
    }
  }
}
