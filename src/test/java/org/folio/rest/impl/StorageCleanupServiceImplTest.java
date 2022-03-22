package org.folio.rest.impl;

import static java.util.Objects.nonNull;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.COMPLETED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.vertx.core.Context;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.FileDefinitionDao;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.service.ApplicationTestConfig;
import org.folio.service.file.cleanup.StorageCleanupService;
import org.folio.spring.SpringContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class StorageCleanupServiceImplTest extends RestVerticleTestBase {

  private static final Logger LOGGER = LogManager.getLogger(StorageCleanupServiceImplTest.class);

  private static final String FILE_DEFINITIONS_TABLE = "file_definitions";
  private static final String STORAGE_PATH = "./storage";
  private static final long ONE_HOUR_ONE_MINUTE_IN_MILLIS = 3660000;
  private static final long FIFTY_NINE_MINUTES_IN_MILLIS = 3540000;
  private static final String FILE_DEFINITION_ID_1 = "b28793ab-4014-4df9-bfed-0b4ccd103712";
  private static final String FILE_DEFINITION_ID_2 = "6065483b-3130-4361-b835-2c4abbb697d2";
  private static final String TEST_FILE_1_NAME = "marc1.mrc";
  public static final String TEST_FILE_2_NAME = "marc2.mrc";

  @Autowired
  private FileDefinitionDao fileDefinitionDao;
  @Autowired
  private StorageCleanupService storageCleanupService;

  private FileDefinition fileDefinition1;
  private FileDefinition fileDefinition2;
  private File testFile1;
  private File testFile2;

  public StorageCleanupServiceImplTest() {
    setUpSpringContext();
  }

  @BeforeEach
  public void setUp(VertxTestContext context) throws IOException {
    super.setUp();
    clearFileDefinitionTable(context);
    createTestFiles();
    createFileDefinitions();
  }

  @AfterEach
  public void tearDownFileSystem() throws IOException {
    removeFileAndParentDirectory(testFile1);
    removeFileAndParentDirectory(testFile2);
    remove(new File(STORAGE_PATH));
  }

  @Test
  void shouldRemoveFile_andFileDefinition_whenFileDefinitionWasUpdatedLaterThanHourAgo(VertxTestContext context) throws IOException {
    // given
    createTestFileAndDirectories(testFile1);
    fileDefinition1.getMetadata()
      .withUpdatedDate(new Date(new Date().getTime() - ONE_HOUR_ONE_MINUTE_IN_MILLIS));

    // when
    fileDefinitionDao.save(fileDefinition1, TENANT_ID).compose(saveAr -> {
      return storageCleanupService.cleanStorage(okapiConnectionParams).onComplete(ar -> {
        context.verify(()->{
          LOGGER.info("succeeded: {}, result: {}, exists: {}, files exists: {}, removed: {}",
            ar.succeeded(), ar.result(), testFile1.exists(), Files.exists(Paths.get(testFile1.getParent())));
          assertTrue(ar.succeeded());
          assertTrue(ar.result());
          assertFalse(testFile1.exists());
          assertFalse(Files.exists(Paths.get(testFile1.getParent())));
          assertFileDefinitionIsRemoved();
          context.completeNow();
        });
      });
    });
  }

  @Test
  void shouldRemoveTwoFiles_andFileDefinitions_whenFileDefinitionsWasUpdatedLaterThanHourAgo(VertxTestContext context) throws IOException {
    // given
    createTestFileAndDirectories(testFile1);
    fileDefinition1.getMetadata()
      .withUpdatedDate(new Date(new Date().getTime() - ONE_HOUR_ONE_MINUTE_IN_MILLIS));
    createTestFileAndDirectories(testFile2);
    fileDefinition2.getMetadata()
      .withUpdatedDate(new Date(new Date().getTime() - ONE_HOUR_ONE_MINUTE_IN_MILLIS));

    // when
    fileDefinitionDao.save(fileDefinition1, TENANT_ID).compose(saveFileDefinition1Ar -> {
      return fileDefinitionDao.save(fileDefinition2, TENANT_ID).compose(saveFileDefinition2Ar -> {
        return storageCleanupService.cleanStorage(okapiConnectionParams).onComplete(ar -> {
          context.verify(() -> {
            assertTrue(ar.succeeded());
            assertTrue(ar.result());
            assertFalse(testFile1.exists());
            assertFalse(testFile2.exists());
            assertFalse(Files.exists(Paths.get(testFile1.getParent())));
            assertFalse(Files.exists(Paths.get(testFile2.getParent())));
            assertFileDefinitionIsRemoved();
            context.completeNow();
          });
        });
      });
    });
  }


  @Test
  void shouldRemoveFileWithoutParentDirectory_whenParentDirectoryIsNull(VertxTestContext context) throws IOException {
    // given
    testFile1 = new File("./" + TEST_FILE_1_NAME);
    createTestFileAndDirectories(testFile1);
    fileDefinition1.withSourcePath("./" + TEST_FILE_1_NAME)
      .getMetadata()
      .withUpdatedDate(new Date(new Date().getTime() - ONE_HOUR_ONE_MINUTE_IN_MILLIS));

    // when
    fileDefinitionDao.save(fileDefinition1, TENANT_ID).compose(saveAr -> {
      return storageCleanupService.cleanStorage(okapiConnectionParams).onComplete(ar -> {
        context.verify(() -> {
          assertTrue(ar.succeeded());
          assertTrue(ar.result());
          assertFalse(testFile1.exists());
          assertFileDefinitionIsRemoved();
          context.completeNow();
        });
      });
    });
  }

  @Test
  void shouldRemoveFileWithoutParentDirectory_whenParentDirectoryIsNotEmpty(VertxTestContext context) throws IOException {
    // given
    createTestFileAndDirectories(testFile1);
    createAnotherFileInFileDefinitionDirectory();
    fileDefinition1.getMetadata()
      .withUpdatedDate(new Date(new Date().getTime() - ONE_HOUR_ONE_MINUTE_IN_MILLIS));

    // when
    fileDefinitionDao.save(fileDefinition1, TENANT_ID).compose(saveAr -> {
      return storageCleanupService.cleanStorage(okapiConnectionParams).onComplete(ar -> {
        context.verify(() -> {
          assertTrue(ar.succeeded());
          assertTrue(ar.result());
          assertFalse(testFile1.exists());
          assertFileDefinitionIsRemoved();
          context.completeNow();
        });
      });
    });
  }

  @Test
  void shouldRemoveFileDefinition_whenFileDoesNotExist_andFileDefinitionWasUpdatedLaterThanHourAgo(VertxTestContext context) {
    // given
    fileDefinition1.getMetadata()
      .withUpdatedDate(new Date(new Date().getTime() - ONE_HOUR_ONE_MINUTE_IN_MILLIS));

    //when
    fileDefinitionDao.save(fileDefinition1, TENANT_ID).compose(saveAr -> {
      return storageCleanupService.cleanStorage(okapiConnectionParams).onComplete(ar -> {
        context.verify(() -> {
          assertTrue(ar.succeeded());
          assertTrue(ar.result());
          assertFileDefinitionIsRemoved();
          context.completeNow();
        });
      });
    });
  }

  @Test
  void shouldNotRemoveFileDefinition_whenSourcePathEmpty_andFileDefinitionWasUpdatedLaterThanHourAgo(VertxTestContext context) {
    // given
    fileDefinition1.withSourcePath(StringUtils.EMPTY)
      .getMetadata()
      .withUpdatedDate(new Date(new Date().getTime() - ONE_HOUR_ONE_MINUTE_IN_MILLIS));

    // when
    fileDefinitionDao.save(fileDefinition1, TENANT_ID).compose(saveAr -> {
      return storageCleanupService.cleanStorage(okapiConnectionParams).onComplete(ar -> {
        context.verify(() -> {
          assertTrue(ar.succeeded());
          assertFalse(ar.result());
          assertFileDefinitionIsNotRemoved();
          context.completeNow();
        });
      });
    });
  }

  @Test
  void shouldNotRemoveFileDefinition_whenSourcePathEmpty_andFileDefinitionWasUpdatedEarlierThanHourAgo(VertxTestContext context) {
    // given
    fileDefinition1.withSourcePath(StringUtils.EMPTY)
      .getMetadata()
      .withUpdatedDate(new Date(new Date().getTime() - FIFTY_NINE_MINUTES_IN_MILLIS));

    // when
    fileDefinitionDao.save(fileDefinition1, TENANT_ID).compose(saveAr -> {
      return storageCleanupService.cleanStorage(okapiConnectionParams).onComplete(ar -> {
        context.verify(() -> {
          assertTrue(ar.succeeded());
          assertFalse(ar.result());
          assertFileDefinitionIsNotRemoved();
          context.completeNow();
        });
      });
    });
  }

  private void clearFileDefinitionTable(VertxTestContext context) {
    PostgresClient.getInstance(vertx, TENANT_ID).delete(FILE_DEFINITIONS_TABLE, new Criterion(), event -> {
      if (event.failed()) {
        context.failNow(event.cause());
      }
    });
    context.completeNow();
  }

  private void setUpSpringContext() {
    Context vertxContext = vertx.getOrCreateContext();
    SpringContextUtil.init(vertxContext.owner(), vertxContext, ApplicationTestConfig.class);
    SpringContextUtil.autowireDependencies(this, vertxContext);
  }

  private void createTestFiles() {
    testFile1 = new File(STORAGE_PATH + "/" + FILE_DEFINITION_ID_1 + "/" + TEST_FILE_1_NAME);
    testFile2 = new File(STORAGE_PATH + "/" + FILE_DEFINITION_ID_2 + "/" + TEST_FILE_2_NAME);
  }

  private void createFileDefinitions() {
    fileDefinition1 = createFileDefinition(FILE_DEFINITION_ID_1, testFile1.getPath(), TEST_FILE_1_NAME);
    fileDefinition2 = createFileDefinition(FILE_DEFINITION_ID_2, testFile2.getPath(), TEST_FILE_2_NAME);
  }

  private FileDefinition createFileDefinition(String fileDefinitionId, String filePath, String fileName) {
    return new FileDefinition()
      .withId(fileDefinitionId)
      .withSourcePath(filePath)
      .withFileName(fileName)
      .withStatus(COMPLETED)
      .withSize(209)
      .withMetadata(new Metadata()
        .withCreatedDate(new Date())
        .withUpdatedDate(new Date()));

  }

  private void createTestFileAndDirectories(File file) throws IOException {
    if (!file.exists()) {
      file.getParentFile().mkdirs();
      file.createNewFile();
    }
  }

  private void createAnotherFileInFileDefinitionDirectory() throws IOException {
    testFile2 = new File(STORAGE_PATH + "/" + FILE_DEFINITION_ID_1 + "/" + TEST_FILE_2_NAME);
    testFile2.createNewFile();
  }

  private void assertFileDefinitionIsRemoved() {
    fileDefinitionDao.getById(FILE_DEFINITION_ID_1, TENANT_ID).onComplete(fileDefinitionAr -> {
      assertTrue(fileDefinitionAr.result().isEmpty());
    });
  }

  private void assertFileDefinitionIsNotRemoved() {
    fileDefinitionDao.getById(FILE_DEFINITION_ID_1, TENANT_ID).onComplete(fileDefinitionAr -> {
      //compare fields and not the POJO, as the object address may be different
      assertEquals(fileDefinitionAr.result().get().getId(), fileDefinition1.getId());
      assertEquals(fileDefinitionAr.result().get().getFileName(), fileDefinition1.getFileName());
      assertEquals(fileDefinitionAr.result().get().getStatus(), fileDefinition1.getStatus());
      assertEquals(fileDefinitionAr.result().get().getSize(), fileDefinition1.getSize());
    });
  }

  private void removeFileAndParentDirectory(File file) {
    remove(file);
    remove(file.getParentFile());
  }

  private void remove(File file) {
    if (nonNull(file) && file.exists()) {
      file.delete();
    }
  }

}
