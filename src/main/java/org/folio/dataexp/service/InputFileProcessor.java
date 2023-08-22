package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.cql2pgjson.exception.QueryValidationException;
import org.folio.cql2pgjson.exception.ServerChoiceIndexesException;
import org.folio.cql2pgjson.model.SqlSelect;
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
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.folio.dataexp.service.file.upload.FileUploadServiceImpl.PATTERN_TO_SAVE_FILE;

@Component
@RequiredArgsConstructor
@Log4j2
public class InputFileProcessor {

  private static final String SQL_FROM_INVENTORY_STORAGE = "SELECT id FROM v_instance ";

  private final ExportIdEntityRepository exportIdEntityRepository;
  private final FolioS3ClientFactory folioS3ClientFactory;
  private final JdbcTemplate jdbcTemplate;
  private final InstanceIdRowMapper instanceIdRowMapper;

  public void readCsvFile(FileDefinition fileDefinition) throws IOException {
    var pathToRead = String.format(PATTERN_TO_SAVE_FILE, fileDefinition.getId(), fileDefinition.getFileName());
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

  public void readCqlFile(FileDefinition fileDefinition) throws IOException, ServerChoiceIndexesException, FieldException, QueryValidationException {
    var pathToRead = String.format(PATTERN_TO_SAVE_FILE, fileDefinition.getId(), fileDefinition.getFileName());
    var s3Client = folioS3ClientFactory.getFolioS3Client();
    String cql;
    try (InputStream is = s3Client.read(pathToRead)) {
      cql = IOUtils.toString(is, StandardCharsets.UTF_8);
    }
    if (cql != null) {
      CQL2PgJSON converter = new CQL2PgJSON("jsonb", Collections.emptyList());
      var query = SQL_FROM_INVENTORY_STORAGE + converter.toSql(cql).toString();
      query = query.replace("f_unaccent", "unaccent");
      var ids = jdbcTemplate.query(query, instanceIdRowMapper);
      ids.forEach(instanceId -> {
        var entity = ExportIdEntity.builder().jobExecutionId(fileDefinition
          .getJobExecutionId()).instanceId(instanceId).build();
        exportIdEntityRepository.save(entity);
      });
    }
  }
}
