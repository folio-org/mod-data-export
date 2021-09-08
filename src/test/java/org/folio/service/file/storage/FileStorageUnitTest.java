package org.folio.service.file.storage;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Lists;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
class FileStorageUnitTest {

  private Vertx vertx = Vertx.vertx();
  private FileStorage fileStorage = new LocalFileSystemStorage(vertx);

  @Test
  void shouldSaveFileDataBlocking() throws IOException {
    // given
    String fileContent = "01240cas a2200397   45000010007000000050";
    FileDefinition givenFileDefinition = new FileDefinition()
      .withId(UUID.randomUUID().toString())
      .withFileName("textFile.txt");
    // when
    FileDefinition savedFileDefinition = fileStorage.saveFileDataBlocking(fileContent.getBytes(), givenFileDefinition);
    // then
    assertNotNull(savedFileDefinition);
    File savedFile = new File(savedFileDefinition.getSourcePath());
    String savedFileContent = new String(Files.readAllBytes(savedFile.toPath()));
    assertEquals(fileContent, savedFileContent);
    assertTrue(fileStorage.isFileExist(savedFileDefinition.getSourcePath()));
    // case when path is null
    String sourcePath = savedFileDefinition.getSourcePath();
    savedFileDefinition.setSourcePath(null);
    assertFalse(fileStorage.isFileExist(savedFileDefinition.getSourcePath()));
    // case when path is empty
    savedFileDefinition.setSourcePath("");
    assertFalse(fileStorage.isFileExist(savedFileDefinition.getSourcePath()));
    savedFileDefinition.setSourcePath(sourcePath);
    // clean up storage
    FileUtils.deleteDirectory(new File("./storage"));
    assertFalse(fileStorage.isFileExist(savedFileDefinition.getSourcePath()));
  }

  @Test
  void shouldSaveFileDataAsyncCQL(VertxTestContext testContext) {
    // given
    List<String> uuids = Lists.newArrayList("uuid");
    FileDefinition givenFileDefinition = new FileDefinition()
      .withId(UUID.randomUUID().toString())
      .withFileName("textFile.txt");
    // when
    Future<FileDefinition> future = fileStorage.saveFileDataAsyncCQL(uuids, givenFileDefinition);
    // then
    future.onComplete(ar -> {
      testContext.verify(() -> {
        assertTrue(ar.succeeded());
        FileDefinition savedFileDefinition = ar.result();
        assertNotNull(savedFileDefinition);
        try {
          File savedFile = new File(savedFileDefinition.getSourcePath());
          String savedFileContent = new String(Files.readAllBytes(savedFile.toPath()));
          assertEquals("uuid", savedFileContent);
          assertTrue(fileStorage.isFileExist(savedFileDefinition.getSourcePath()));
          // clean up storage
          FileUtils.deleteDirectory(new File("./storage"));
          assertFalse(fileStorage.isFileExist(savedFileDefinition.getSourcePath()));
          testContext.completeNow();
        } catch (IOException e) {
          testContext.failNow(e);
        }
      });

    });
  }

  @Test
  void shouldSaveFileDataAsynchronously(VertxTestContext testContext) {
    // given
    String fileContent = "01240cas a2200397   45000010007000000050";
    FileDefinition givenFileDefinition = new FileDefinition()
      .withId(UUID.randomUUID().toString())
      .withFileName("textFile.txt");
    // when
    Future<FileDefinition> future = fileStorage.saveFileDataAsync(fileContent.getBytes(), givenFileDefinition);
    // then
    future.onComplete(ar -> {
      testContext.verify(() -> {
        assertTrue(ar.succeeded());
        FileDefinition savedFileDefinition = ar.result();
        assertNotNull(savedFileDefinition);
        try {
          File savedFile = new File(savedFileDefinition.getSourcePath());
          String savedFileContent = new String(Files.readAllBytes(savedFile.toPath()));
          assertEquals(fileContent, savedFileContent);
          // clean up storage
          FileUtils.deleteDirectory(new File("./storage"));
          assertFalse(fileStorage.isFileExist(savedFileDefinition.getSourcePath()));
          testContext.completeNow();
        } catch (IOException e) {
          testContext.failNow(e);
        }
      });

    });
  }

  @Test
  void shouldThrowException_whenWrongFileDefinitionPassed() {
    // given
    FileDefinition givenFileDefinition = new FileDefinition();
    // when
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      fileStorage.saveFileDataBlocking(new byte[]{}, givenFileDefinition);
    });
    // then expect IllegalArgumentException
  }

}
