package org.folio.dataexp.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.IdType;
import org.folio.dataexp.exception.export.DownloadRecordException;
import org.folio.dataexp.service.export.ExportStrategyFactory;
import org.folio.dataexp.service.export.strategies.JsonToMarcConverter;
import org.folio.spring.FolioExecutionContext;
import org.marc4j.MarcException;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.converter.impl.UnicodeToAnsel;
import org.marc4j.marc.DataField;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

/** Service for downloading MARC records by ID. */
@Service
@AllArgsConstructor
@Log4j2
public class DownloadRecordService {

  private final ExportStrategyFactory exportStrategyFactory;
  private final JsonToMarcConverter jsonToMarcConverter;
  protected final FolioExecutionContext folioExecutionContext;

  /**
   * Processes the download of a record by its ID. The MARC file is generated from the current
   * record on every request so that updates to the record are always reflected.
   *
   * @param recordId The record UUID.
   * @param isUtf Whether to use UTF encoding.
   * @param idType The type of ID.
   * @param suppress999ff Whether to remove the 999 ff field.
   * @return InputStreamResource containing the record data.
   */
  public InputStreamResource processRecordDownload(
      final UUID recordId, boolean isUtf, final IdType idType, boolean suppress999ff) {
    log.info(
        "processRecordDownload:: start downloading record with id: {}, "
            + "isUtf: {}, suppress999ff: {}",
        recordId,
        isUtf,
        suppress999ff);
    InputStream marcFileContent =
        new ByteArrayInputStream(generateRecordFileContentBytes(recordId, isUtf, idType));
    if (suppress999ff) {
      marcFileContent = remove999ffField(isUtf, marcFileContent);
    }
    return new InputStreamResource(marcFileContent);
  }

  /**
   * Generates the MARC file content bytes for a record.
   *
   * @param recordId The record UUID.
   * @param isUtf Whether to use UTF encoding.
   * @param idType The type of ID.
   * @return Byte array of the MARC file content.
   */
  private byte[] generateRecordFileContentBytes(
      final UUID recordId, boolean isUtf, final IdType idType) {
    var exportStrategy = exportStrategyFactory.getExportStrategy(idType);
    var marcRecord = exportStrategy.getMarcRecord(recordId);
    var mappingProfile = exportStrategy.getDefaultMappingProfile();
    try {
      return jsonToMarcConverter
          .convertJsonRecordToMarcRecord(marcRecord.getContent(), List.of(), mappingProfile, isUtf)
          .toByteArray();
    } catch (IOException | MarcException e) {
      log.error(
          "generateRecordFileContentBytes:: Error generating content for record with ID: {}",
          recordId, e);
      throw new DownloadRecordException(e.getMessage());
    }
  }

  private InputStream remove999ffField(boolean isUtf, InputStream marcFileContent) {
    try (var marcOutputStream = new ByteArrayOutputStream()) {
      var marcWriter = new MarcStreamWriter(marcOutputStream, StandardCharsets.UTF_8.name());
      if (!isUtf) {
        marcWriter.setConverter(new UnicodeToAnsel());
      }
      MarcReader marcReader = new MarcStreamReader(marcFileContent);
      while (marcReader.hasNext()) {
        var marcRecord = marcReader.next();
        var fieldToRemove =
            marcRecord.getVariableFields().stream()
                .filter(
                    vf ->
                        vf instanceof DataField df
                            && df.getTag().equals("999")
                            && df.getIndicator1() == 'f'
                            && df.getIndicator2() == 'f')
                .findFirst();
        fieldToRemove.ifPresent(marcRecord::removeVariableField);
        marcWriter.write(marcRecord);
      }
      return new ByteArrayInputStream(marcOutputStream.toByteArray());
    } catch (IOException e) {
      log.error("Failed to remove tag 999ff from marc record", e);
      return marcFileContent;
    }
  }
}
