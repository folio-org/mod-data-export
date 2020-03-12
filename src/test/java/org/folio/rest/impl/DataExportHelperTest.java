package org.folio.rest.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import io.vertx.core.Promise;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import org.folio.rest.exceptions.HttpException;
import org.folio.rest.jaxrs.model.ExportedFile;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.service.job.JobExecutionService;
import org.folio.util.ErrorCodes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 *  Tests are written in blocking manner while testing the async code, that causes unstable builds on master branch sometimes.
 *  We need to overwrite all this tests using VertxUnitRunner instead of MockitoJunitRunner.
 *  See org.folio.service.export.storage.ExportStorageServiceUnitTest for example.
 */
@RunWith(MockitoJUnitRunner.class)
public class DataExportHelperTest {
  @Mock
  private ExportStorageService exportStorageService;
  @Mock
  private JobExecutionService jobExecutionServiceMock;
  @InjectMocks
  @Spy
  private DataExportHelper helper = new DataExportHelper();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testSuccessfulGetDownloadLink() throws MalformedURLException {

    ExportStorageService storageServiceMock = Mockito.mock(ExportStorageService.class);

    String url = "https://test.aws.amazon.com";
    Promise<String> pr = Promise.promise();
    pr.complete("test.mrc");
    doReturn(pr.future()).when(helper)
      .getDownloadFileName(anyString(), anyString(), anyString());

    Promise<String> pr1 = Promise.promise();
    pr1.complete(url);

    doReturn(pr1.future()).when(storageServiceMock)
      .getFileDownloadLink(anyString(), anyString(), anyString());

    assertNotNull(helper.getDownloadLink(anyString(), anyString(), anyString())
      .result()
      .getLink());

  }

  @Test
  public void testFailGetFileDownloadLink() throws MalformedURLException {
    ExportStorageService storageServiceMock = Mockito.mock(ExportStorageService.class);
    Promise<String> pr = Promise.promise();
    pr.complete("test.mrc");
    doReturn(pr.future()).when(helper)
      .getDownloadFileName(anyString(), anyString(), anyString());

    doThrow(new HttpException(400, ErrorCodes.S3_BUCKET_NOT_PROVIDED)).when(storageServiceMock)
      .getFileDownloadLink(anyString(), anyString(), anyString());

    assertEquals(true, helper.getDownloadLink(anyString(), anyString(), anyString())
      .failed());

    assertNull(helper.getDownloadLink(anyString(), anyString(), anyString())
      .result());
  }

  @Test
  public void testSuccessfulGetDownloadFileName() throws MalformedURLException {

    String fileName = "test-20200311100503.mrc";

    JobExecution jobExecution = new JobExecution();
    ExportedFile exportedFile = new ExportedFile();
    String fileId = UUID.randomUUID()
        .toString();
    exportedFile.withFileId(fileId)
      .withFileName(fileName);
    HashSet<ExportedFile> filesSet = new HashSet<>();

    filesSet.add(exportedFile);
    jobExecution.withId(UUID.randomUUID().toString())
      .withExportedFiles(filesSet)
      .withStatus(JobExecution.Status.SUCCESS);
    System.err.println(jobExecution);

    Promise<Optional<JobExecution>> pr = Promise.promise();
    pr.complete(Optional.of(jobExecution));

    doReturn(pr.future()).when(jobExecutionServiceMock).getById(anyString(), anyString());

    assertEquals(fileName, helper.getDownloadFileName("1234", fileId, "test")
      .result());

  }

  @Test
  public void testFailGetDownloadFileNameNoJobId() throws MalformedURLException {

    Promise<Optional<JobExecution>> pr = Promise.promise();
    pr.complete(Optional.empty());

    doReturn(pr.future()).when(jobExecutionServiceMock).getById(anyString(), anyString());

    assertEquals(true, helper.getDownloadFileName(anyString(), anyString(), anyString())
      .failed());

  }

}
