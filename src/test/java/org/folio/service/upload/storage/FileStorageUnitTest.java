package org.folio.service.upload.storage;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.FileUtils;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
public class FileStorageUnitTest {

  private Vertx vertx = Vertx.vertx();
  private FileStorage fileStorage = new LocalFileSystemStorage(vertx);

  @Test
  public void shouldSaveFileDataBlocking(TestContext testContext) throws IOException {
    // given
    String fileContent = "01240cas a2200397   45000010007000000050";
    FileDefinition givenFileDefinition = new FileDefinition()
      .withId(UUID.randomUUID().toString())
      .withFileName("textFile.txt");
    // when
    FileDefinition savedFileDefinition = fileStorage.saveFileDataBlocking(fileContent.getBytes(), givenFileDefinition);
    // then
    testContext.assertNotNull(savedFileDefinition);
    File savedFile = new File(savedFileDefinition.getSourcePath());
    String savedFileContent = new String(Files.readAllBytes(savedFile.toPath()));
    assertEquals(fileContent, savedFileContent);
    // clean up storage
    FileUtils.deleteDirectory(new File("./storage"));
  }

  @Test
  public void shouldSaveFileDataAsynchronously(TestContext testContext) {
    // given
    Async async = testContext.async();
    String fileContent = "01240cas a2200397   45000010007000000050";
    FileDefinition givenFileDefinition = new FileDefinition()
      .withId(UUID.randomUUID().toString())
      .withFileName("textFile.txt");
    // when
    Future<FileDefinition> future = fileStorage.saveFileDataAsync(fileContent.getBytes(), givenFileDefinition);
    // then
    future.setHandler(ar -> {
      testContext.assertTrue(ar.succeeded());
      FileDefinition savedFileDefinition = ar.result();
      testContext.assertNotNull(savedFileDefinition);
      try {
        File savedFile = new File(savedFileDefinition.getSourcePath());
        String savedFileContent = new String(Files.readAllBytes(savedFile.toPath()));
        assertEquals(fileContent, savedFileContent);
        // clean up storage
        FileUtils.deleteDirectory(new File("./storage"));
      } catch (IOException e) {
        testContext.fail();
      }
      async.complete();
    });
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionIfWrongFileDefinitionPassed(TestContext testContext) {
    // given
    FileDefinition givenFileDefinition = new FileDefinition();
    // when
    FileDefinition savedFileDefinition = fileStorage.saveFileDataBlocking(new byte[]{}, givenFileDefinition);
    // then expect IllegalArgumentException
  }
}
