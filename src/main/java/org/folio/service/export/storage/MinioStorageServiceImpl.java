package org.folio.service.export.storage;

import static java.lang.System.getProperty;
import static org.folio.service.export.storage.ClientFactory.BUCKET_PROP_KEY;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
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
import org.springframework.stereotype.Service;

import com.amazonaws.util.StringUtils;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.UploadObjectArgs;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
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
  private ClientFactory clientFactory;
  @Autowired
  private Vertx vertx;
  @Autowired
  private ErrorLogService errorLogService;

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
    var client = clientFactory.getClient();
    var keyName = tenantId + "/" + jobExecutionId + "/" + exportFileName;
    var bucketName = getProperty(BUCKET_PROP_KEY);

    if (StringUtils.isNullOrEmpty(bucketName)) {
      errorLogService.saveGeneralError(ErrorCode.S3_BUCKET_IS_NOT_PROVIDED.getCode(), jobExecutionId, tenantId);
      throw new ServiceException(HttpStatus.HTTP_INTERNAL_SERVER_ERROR, ErrorCode.S3_BUCKET_NAME_NOT_FOUND);
    }

    var generatePresignedUrlRequest = GetPresignedObjectUrlArgs.builder()
      .bucket(bucketName)
      .object(keyName)
      .method(Method.GET)
      .expiry(EXPIRATION_TIME_IN_MINUTES, TimeUnit.MINUTES);

    vertx.executeBlocking(blockingFuture -> {
      String url = null;
      try {
        url = client.getPresignedObjectUrl(generatePresignedUrlRequest.build());
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
    var folderInS3 = tenantId + "/" + fileDefinition.getJobExecutionId();
    var bucketName = getProperty(BUCKET_PROP_KEY);
    if (StringUtils.isNullOrEmpty(bucketName)) {
      errorLogService.saveGeneralError(ErrorCode.S3_BUCKET_NAME_NOT_FOUND.getCode(), fileDefinition.getJobExecutionId(), tenantId);
      throw new ServiceException(HttpStatus.HTTP_INTERNAL_SERVER_ERROR, ErrorCode.S3_BUCKET_NAME_NOT_FOUND);
    } else {
      var client = clientFactory.getClient();
      try {
        Files.walk(Paths.get(fileDefinition.getSourcePath())
          .getParent())
          .filter(Files::isRegularFile)
          .forEach(file -> {
            try {
              var uploadObjectArgs = UploadObjectArgs.builder()
                .bucket(bucketName)
                .object(file.getName(file.getNameCount() - 1)
                  .toString())
                .filename(file.toString());
              client.uploadObject(uploadObjectArgs.build());
            } catch (Exception e) {
              throw new ServiceException(HttpStatus.HTTP_INTERNAL_SERVER_ERROR, e.getMessage());
            }
          });
      } catch (IOException e) {
        throw new ServiceException(HttpStatus.HTTP_INTERNAL_SERVER_ERROR, e.getMessage());
      }
    }
  }

  @Override
  public void removeFilesRelatedToJobExecution(JobExecution jobExecution, String tenantId) {
    var bucketName = getProperty(BUCKET_PROP_KEY);
    if (StringUtils.isNullOrEmpty(bucketName)) {
      throw new ServiceException(HttpStatus.HTTP_NOT_FOUND, ErrorCode.S3_BUCKET_NAME_NOT_FOUND.getDescription());
    }
    if (CollectionUtils.isNotEmpty(jobExecution.getExportedFiles())) {
      var client = clientFactory.getClient();
      var listObjectsArgs = ListObjectsArgs.builder()
        .bucket(bucketName)
        .prefix(tenantId + "/" + jobExecution.getId())
        .build();

      var objectList = client.listObjects(listObjectsArgs);
      List<DeleteObject> objects = new LinkedList<>();

      objectList.forEach(obj -> {
        try {
          objects.add(new DeleteObject(obj.get()
            .objectName()));
        } catch (Exception e) {
          throw new ServiceException(HttpStatus.HTTP_INTERNAL_SERVER_ERROR, e.getMessage());
        }
      });

      var multiObjectDeleteRequest = RemoveObjectsArgs.builder()
        .bucket(bucketName)
        .objects(objects)
        .build();

      client.removeObjects(multiObjectDeleteRequest);

    } else {
      LOGGER.error("No exported files is present related to jobExecution with id {}", jobExecution.getId());
    }
  }
}
