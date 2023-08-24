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
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.service.export.storage.FolioS3ClientFactory;
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
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import static org.folio.dataexp.service.file.upload.FileUploadServiceImpl.PATTERN_TO_SAVE_FILE;

@Component
@RequiredArgsConstructor
@Log4j2
public class InputFileProcessor {

  private static final String CALL_SAVE_INSTANCES_IDS_PROCEDURE = "call save_instances_ids(?, ?)";

  private final ExportIdEntityRepository exportIdEntityRepository;
  private final FolioS3ClientFactory folioS3ClientFactory;
  private final JdbcTemplate jdbcTemplate;

  public void readCsvFile(FileDefinition fileDefinition) throws IOException {
    var pathToRead = getPathToRead(fileDefinition);
    var s3Client = folioS3ClientFactory.getFolioS3Client();
    try (InputStream is = s3Client.read(pathToRead); BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
        reader.lines().forEach(id -> {
        var instanceId = id.replace("\"", StringUtils.EMPTY);
        var entity = ExportIdEntity.builder().jobExecutionId(fileDefinition
          .getJobExecutionId()).instanceId(UUID.fromString(instanceId)).build();
        exportIdEntityRepository.save(entity);
      });
    }
  }

  public void readCqlFile(FileDefinition fileDefinition) throws IOException, ServerChoiceIndexesException, FieldException, QueryValidationException, SQLException {
    var pathToRead = getPathToRead(fileDefinition);
    var s3Client = folioS3ClientFactory.getFolioS3Client();
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

  private String getPathToRead(FileDefinition fileDefinition) {
    return String.format(PATTERN_TO_SAVE_FILE, fileDefinition.getId(), fileDefinition.getFileName());
  }
}
