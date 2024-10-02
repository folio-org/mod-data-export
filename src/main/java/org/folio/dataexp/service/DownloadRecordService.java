package org.folio.dataexp.service;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Log4j2
public class DownloadRecordService {

  private final ExportStrategyFactory exportStrategyFactory;
  private final JsonToMarcConverter jsonToMarcConverter;
  private final S3ExportsUploader s3Uploader;
  private final InputFileProcessor inputFileProcessor;
  protected final FolioExecutionContext folioExecutionContext;

  public InputStreamResource processRecordDownload(final UUID recordId,
                                                 boolean isUtf,
                                                 final String formatPostfix,
                                                 final IdType idType) {
    log.info("processRecordDownload:: start downloading record with id: {}, isUtf: {}", recordId, isUtf);
    var dirName = recordId.toString() + formatPostfix;
    InputStream marcFileContent = getContentIfFileExists(dirName);
    if (marcFileContent == null) {
      byte[] marcFileContentBytes = generateRecordFileContentBytes(recordId, isUtf, idType);
      uploadMarcFile(dirName, marcFileContentBytes);
      return new InputStreamResource(new ByteArrayInputStream(marcFileContentBytes));
    }
    else {
      return new InputStreamResource(marcFileContent);
    }

  }

  private InputStream getContentIfFileExists(final String dirName) {
    return inputFileProcessor.readMarcFile(dirName);
  }

  private byte[] generateRecordFileContentBytes(final UUID recordId, boolean isUtf, final IdType idType) {
    var exportStrategy = exportStrategyFactory.getExportStrategy(idType);
    var marcRecord = exportStrategy.getMarcRecord(recordId);
    var mappingProfile = exportStrategy.getDefaultMappingProfile();
    try {
      return jsonToMarcConverter.convertJsonRecordToMarcRecord(marcRecord.getContent(), List.of(), mappingProfile,
        isUtf).toByteArray();
    } catch (IOException e) {
      log.error("generateRecordFileContent :: Error generating content for record with ID: {}", recordId);
      throw new DownloadRecordException(e.getMessage());
    }
  }

  private void uploadMarcFile(final String dirName, byte[] marcFileContentBytes) {
    try {
      s3Uploader.uploadSingleRecordById(dirName, marcFileContentBytes);
    } catch (IOException e) {
      log.error("uploadMarcFile:: Error while upload marc file to remote storage {}", dirName);
      throw new DownloadRecordException(e.getMessage());
    }
  }
}
