package org.folio.service.export.storage;

import java.io.BufferedInputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.service.logs.ErrorLogService;
import org.folio.util.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.util.StringUtils;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

/**
 * Saves files into Amazon cloud and provides access for files being stored there via MinIO
 */
@Service
public class MinioStorageServiceImpl implements ExportStorageService {

  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  private static final int EXPIRATION_TIME_IN_MINUTES = 10;

  @Autowired
  private MinioClientFactory minioClientFactory;
  @Autowired
  private Vertx vertx;
  @Autowired
  private ErrorLogService errorLogService;

  @Value("${minio.bucket}")
  private String bucket;

  /**
   * Fetch the link to download a file for a given job by fileName
   *
   * @param jobExecutionId The job to which the files are associated
   * @param exportFileName The name of the file to download
   * @param tenantId       tenant id
   * @return A link using which the file can be downloaded
   */
  @Override
  public Future<String> getFileDownloadLink(String jobExecutionId, String exportFileName, String tenantId) {
    Promise<String> promise = Promise.promise();
    var client = minioClientFactory.getClient();
    var key = buildPrefix(tenantId, jobExecutionId) + "/" + exportFileName;

    if (StringUtils.isNullOrEmpty(bucket)) {
      errorLogService.saveGeneralError(ErrorCode.S3_BUCKET_IS_NOT_PROVIDED.getCode(), jobExecutionId, tenantId);
      throw new ServiceException(HttpStatus.HTTP_INTERNAL_SERVER_ERROR, ErrorCode.S3_BUCKET_NAME_NOT_FOUND);
    }

    vertx.executeBlocking(blockingFuture -> {
      String url = null;
      try {
        url = client.getPresignedObjectUrl(getGetPresignedObjectUrlArgs(key, bucket));
      } catch (Exception e) {
        blockingFuture.fail(e);
      }
      blockingFuture.complete(url);
    }, asyncResult -> {
      if (asyncResult.failed()) {
        promise.fail(asyncResult.cause());
      } else {
        String url = (String) asyncResult.result();
        promise.complete(url);
      }
    });

    return promise.future();
  }

  @Override
  public void storeFile(FileDefinition fileDefinition, String tenantId) {
    var folderToSave = buildPrefix(tenantId, fileDefinition.getJobExecutionId());
    if (StringUtils.isNullOrEmpty(bucket)) {
      errorLogService.saveGeneralError(ErrorCode.S3_BUCKET_NAME_NOT_FOUND.getCode(), fileDefinition.getJobExecutionId(), tenantId);
      throw new ServiceException(HttpStatus.HTTP_INTERNAL_SERVER_ERROR, ErrorCode.S3_BUCKET_NAME_NOT_FOUND);
    } else {
      var client = minioClientFactory.getFolioS3Client();
      vertx.fileSystem()
        .readDirBlocking(Paths.get(fileDefinition.getSourcePath())
          .getParent()
          .toString())
        .stream()
        .map(Paths::get)
        .filter(Files::isRegularFile)
        .forEach(filePath -> {
          try (var is =  new BufferedInputStream(Files.newInputStream(filePath))) {
            var path = folderToSave + "/" + filePath.getName(filePath.getNameCount() - 1);
            client.write(path, is);
          } catch (Exception e) {
            LOGGER.warn("storeFile:: Error during storing file for jobExecution {} with message {} ",  fileDefinition.getJobExecutionId(), e.getMessage());
            throw new ServiceException(HttpStatus.HTTP_INTERNAL_SERVER_ERROR, e.getMessage());
         }
        });
    }
  }

  @Override
  public void removeFilesRelatedToJobExecution(JobExecution jobExecution, String tenantId) {
    if (StringUtils.isNullOrEmpty(bucket)) {
      throw new ServiceException(HttpStatus.HTTP_NOT_FOUND, ErrorCode.S3_BUCKET_NAME_NOT_FOUND.getDescription());
    }
    if (CollectionUtils.isNotEmpty(jobExecution.getExportedFiles())) {
      var client = minioClientFactory.getFolioS3Client();
      var objectList = client.list(buildPrefix(tenantId, jobExecution.getId()));
      objectList.forEach(client::remove);
    } else {
      LOGGER.error("No exported files is present related to jobExecution with id {}", jobExecution.getId());
    }
  }

  private GetPresignedObjectUrlArgs getGetPresignedObjectUrlArgs(String key, String bucket) {
    return GetPresignedObjectUrlArgs.builder()
      .bucket(bucket)
      .object(key)
      .method(Method.GET)
      .expiry(EXPIRATION_TIME_IN_MINUTES, TimeUnit.MINUTES)
      .build();
  }

  private String buildPrefix(String tenantId, String jobExecutionId) {
    return tenantId + "/" + jobExecutionId;
  }
}
