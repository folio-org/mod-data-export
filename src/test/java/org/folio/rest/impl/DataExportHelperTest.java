package org.folio.rest.impl;

import io.vertx.core.Future;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.HttpStatus;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.ExportedFile;
import org.folio.rest.jaxrs.model.FileDownload;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.service.job.JobExecutionService;
import org.folio.util.ErrorCode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.UUID;

import static io.vertx.core.Future.succeededFuture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(VertxUnitRunner.class)
public class DataExportHelperTest {
  @Mock
  private ExportStorageService exportStorageService;
  @Mock
  private JobExecutionService jobExecutionService;
  @InjectMocks
  @Spy
  private DataExportHelper helper = new DataExportHelper();
  private final static String TENANT = "testTenant";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void getDownloadLink_shouldSuccessfulGetDownloadLink(TestContext testContext) throws MalformedURLException {
    Async async = testContext.async();
    // given
    ExportedFile exportedFile = new ExportedFile().withFileId(UUID.randomUUID().toString()).withFileName("testFile-timestemp.mrc");
    JobExecution jobExecution = new JobExecution().withExportedFiles(Collections.singleton(exportedFile));

    String url = "https://test.aws.amazon.com";
    Mockito.when(exportStorageService.getFileDownloadLink(anyString(), anyString(), anyString())).thenReturn(succeededFuture(url));
    Mockito.when(jobExecutionService.getById(anyString(), anyString())).thenReturn(succeededFuture(jobExecution));

    // when
    Future<FileDownload> linkFuture = helper.getDownloadLink(UUID.randomUUID().toString(), exportedFile.getFileId(), TENANT);
    // then
    linkFuture.setHandler(ar -> {
      assertTrue(ar.succeeded());
      assertEquals(url, ar.result().getLink());
      async.complete();
    });
  }

  @Test
  public void getDownloadLink_shouldFailGetFileDownloadLink(TestContext testContext) throws MalformedURLException {
    Async async = testContext.async();
    // given
    Mockito
      .when(exportStorageService.getFileDownloadLink(anyString(), anyString(), anyString()))
      .thenThrow(new ServiceException(HttpStatus.HTTP_BAD_REQUEST, ErrorCode.S3_BUCKET_NOT_PROVIDED));

    ExportedFile exportedFile = new ExportedFile().withFileId(UUID.randomUUID().toString()).withFileName("testFile-timestemp.mrc");
    JobExecution jobExecution = new JobExecution().withExportedFiles(Collections.singleton(exportedFile));
    Mockito
      .when(jobExecutionService.getById(anyString(), anyString()))
      .thenReturn(succeededFuture(jobExecution));

    // when
    Future<FileDownload> linkFuture = helper.getDownloadLink(UUID.randomUUID().toString(), exportedFile.getFileId(), TENANT);
    // then
    linkFuture.setHandler(ar -> {
      assertTrue(ar.failed());
      async.complete();
    });
  }

  @Test
  public void getDownloadLink_shouldFailIfFileIdFound(TestContext testContext) throws MalformedURLException {
    Async async = testContext.async();
    // given
    Mockito
      .when(exportStorageService.getFileDownloadLink(anyString(), anyString(), anyString()))
      .thenThrow(new ServiceException(HttpStatus.HTTP_BAD_REQUEST, ErrorCode.S3_BUCKET_NOT_PROVIDED));

    ExportedFile exportedFile = new ExportedFile().withFileId(UUID.randomUUID().toString()).withFileName("testFile-timestemp.mrc");
    JobExecution jobExecution = new JobExecution().withExportedFiles(Collections.singleton(exportedFile));
    Mockito
      .when(jobExecutionService.getById(anyString(), anyString()))
      .thenReturn(succeededFuture(jobExecution));

    // when
    Future<FileDownload> linkFuture = helper.getDownloadLink(UUID.randomUUID().toString(), UUID.randomUUID().toString(), TENANT);
    // then
    linkFuture.setHandler(ar -> {
      assertTrue(ar.failed());
      async.complete();
    });
  }
}
