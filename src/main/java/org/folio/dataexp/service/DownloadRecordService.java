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
import org.folio.dataexp.service.export.S3ExportsUploader;
import org.folio.dataexp.service.export.strategies.JsonToMarcConverter;
import org.folio.spring.FolioExecutionContext;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.converter.impl.UnicodeToAnsel;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

/**
 * Service for downloading MARC records by ID.
 */
@Service
@AllArgsConstructor
@Log4j2
public class DownloadRecordService {

  private final ExportStrategyFactory exportStrategyFactory;
  private final JsonToMarcConverter jsonToMarcConverter;
  private final S3ExportsUploader s3Uploader;
  private final InputFileProcessor inputFileProcessor;
  protected final FolioExecutionContext folioExecutionContext;

  /**
   * Processes the download of a record by its ID.
   *
   * @param recordId The record UUID.
   * @param isUtf Whether to use UTF encoding.
   * @param formatPostfix Format postfix for the file.
   * @param idType The type of ID.
   * @return InputStreamResource containing the record data.
   */
  public InputStreamResource processRecordDownload(
      final UUID recordId,
      boolean isUtf,
      final String formatPostfix,
      final IdType idType,
      boolean suppress999
  ) {
    log.info(
        "processRecordDownload:: start downloading record with id: {}, isUtf: {}, suppress999: {}",
        recordId,
        isUtf,
        suppress999
    );
    var dirName = recordId.toString() + formatPostfix;
    InputStream marcFileContent = getContentIfFileExists(dirName);
    if (marcFileContent == null) {
      byte[] marcFileContentBytes = generateRecordFileContentBytes(recordId, isUtf, idType);
      uploadMarcFile(dirName, marcFileContentBytes);
      return new InputStreamResource(new ByteArrayInputStream(marcFileContentBytes));
    } else {
      if (suppress999) {
        marcFileContent = removeFieldByTag("999", isUtf, marcFileContent);
      }
      return new InputStreamResource(marcFileContent);
    }
  }

  /**
   * Gets the content of a MARC file if it exists.
   *
   * @param dirName Directory name.
   * @return InputStream of the file, or null if not found.
   */
  private InputStream getContentIfFileExists(final String dirName) {
    return inputFileProcessor.readMarcFile(dirName);
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
      final UUID recordId,
      boolean isUtf,
      final IdType idType
  ) {
    var exportStrategy = exportStrategyFactory.getExportStrategy(idType);
    var marcRecord = exportStrategy.getMarcRecord(recordId);
    var mappingProfile = exportStrategy.getDefaultMappingProfile();
    try {
      return jsonToMarcConverter.convertJsonRecordToMarcRecord(
          marcRecord.getContent(),
          List.of(),
          mappingProfile,
          isUtf
      ).toByteArray();
    } catch (IOException e) {
      log.error(
          "generateRecordFileContent :: Error generating content for record with ID: {}",
          recordId
      );
      throw new DownloadRecordException(e.getMessage());
    }
  }

  /**
   * Uploads the MARC file to remote storage.
   *
   * @param dirName Directory name.
   * @param marcFileContentBytes Byte array of MARC file content.
   */
  private void uploadMarcFile(final String dirName, byte[] marcFileContentBytes) {
    try {
      s3Uploader.uploadSingleRecordById(dirName, marcFileContentBytes);
    } catch (IOException e) {
      log.error(
          "uploadMarcFile:: Error while upload marc file to remote storage {}",
          dirName
      );
      throw new DownloadRecordException(e.getMessage());
    }
  }

  private InputStream removeFieldByTag(String tag, boolean isUtf, InputStream marcFileContent) {
    try (var marcOutputStream = new ByteArrayOutputStream()) {
      var marcWriter = new MarcStreamWriter(marcOutputStream, StandardCharsets.UTF_8.name());
      if (!isUtf) {
        marcWriter.setConverter(new UnicodeToAnsel());
      }
      MarcReader marcReader = new MarcStreamReader(marcFileContent);
      while (marcReader.hasNext()) {
        var record = marcReader.next();
        var fieldToRemove = record.getVariableFields().stream()
                .filter(vf -> vf.getTag().equals(tag)).findFirst();
        fieldToRemove.ifPresent(record::removeVariableField);
        marcWriter.write(record);
      }
      return new ByteArrayInputStream(marcOutputStream.toByteArray());
    } catch (IOException e) {
      log.error("Failed to remove tag {} from marc record: {}", tag, e.getMessage());
      return marcFileContent;
    }
  }
}
