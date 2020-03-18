package org.folio.service.file.cleanup;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.dao.FileDefinitionDao;
import org.folio.rest.RestVerticleTestBase;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.service.ApplicationTestConfig;
import org.folio.spring.SpringContextUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import static java.util.Objects.nonNull;
import static org.drools.core.util.StringUtils.EMPTY;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.COMPLETED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class StorageCleanupServiceImplTest extends RestVerticleTestBase {

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

  @Before
  public void setUp(TestContext context) throws IOException {
    super.setUp(context);
    clearFileDefinitionTable(context);
    createTestFiles();
    createFileDefinitions();
  }

  @After
  public void tearDownFileSystem() throws IOException {
    removeFileAndParentDirectory(testFile1);
    removeFileAndParentDirectory(testFile2);
    remove(new File(STORAGE_PATH));
  }

  @Test
  public void shouldRemoveFile_andFileDefinition_whenFileDefinitionWasUpdatedLaterThanHourAgo(TestContext context) throws IOException {
    // given
    Async async = context.async();
    createTestFileAndDirectories(testFile1);
    fileDefinition1.getMetadata()
      .withUpdatedDate(new Date(new Date().getTime() - ONE_HOUR_ONE_MINUTE_IN_MILLIS));

    // when
    fileDefinitionDao.save(fileDefinition1, TENANT_ID).compose(saveAr -> {
      Future<Boolean> future = storageCleanupService.cleanStorage(okapiConnectionParams);

      // then
      future.setHandler(ar -> {
        assertTrue(ar.succeeded());
        assertTrue(ar.result());
        assertFalse(testFile1.exists());
        assertFalse(Files.exists(Paths.get(testFile1.getParent())));
        assertFileDefinitionIsRemoved();
        async.complete();
      });
      return Promise.promise().future();
    });
  }

  @Test
  public void shouldRemoveTwoFiles_andFileDefinitions_whenFileDefinitionsWasUpdatedLaterThanHourAgo(TestContext context) throws IOException {
    // given
    Async async = context.async();
    createTestFileAndDirectories(testFile1);
    fileDefinition1.getMetadata()
      .withUpdatedDate(new Date(new Date().getTime() - ONE_HOUR_ONE_MINUTE_IN_MILLIS));
    createTestFileAndDirectories(testFile2);
    fileDefinition2.getMetadata()
      .withUpdatedDate(new Date(new Date().getTime() - ONE_HOUR_ONE_MINUTE_IN_MILLIS));

    // when
    fileDefinitionDao.save(fileDefinition1, TENANT_ID).compose(saveFileDefinition1Ar -> {
      return fileDefinitionDao.save(fileDefinition2, TENANT_ID).compose(saveFileDefinition2Ar -> {
        Future<Boolean> future = storageCleanupService.cleanStorage(okapiConnectionParams);

        // then
        future.setHandler(ar -> {
          assertTrue(ar.succeeded());
          assertTrue(ar.result());
          assertFalse(testFile1.exists());
          assertFalse(testFile2.exists());
          assertFalse(Files.exists(Paths.get(testFile1.getParent())));
          assertFalse(Files.exists(Paths.get(testFile2.getParent())));
          assertFileDefinitionIsRemoved();
          async.complete();
        });
        return Promise.promise().future();
      });
    });
  }

  @Test
  public void shouldRemoveFileWithoutParentDirectory_whenParentDirectoryIsNull(TestContext context) throws IOException {
    // given
    Async async = context.async();
    testFile1 = new File("./" + TEST_FILE_1_NAME);
    createTestFileAndDirectories(testFile1);
    fileDefinition1.withSourcePath("./" + TEST_FILE_1_NAME)
      .getMetadata()
      .withUpdatedDate(new Date(new Date().getTime() - ONE_HOUR_ONE_MINUTE_IN_MILLIS));

    // when
    fileDefinitionDao.save(fileDefinition1, TENANT_ID).compose(saveAr -> {
      Future<Boolean> future = storageCleanupService.cleanStorage(okapiConnectionParams);

      // then
      future.setHandler(ar -> {
        assertTrue(ar.succeeded());
        assertTrue(ar.result());
        assertFalse(testFile1.exists());
        assertFileDefinitionIsRemoved();
        async.complete();
      });
      return Promise.promise().future();
    });
  }

  @Test
  public void shouldRemoveFileWithoutParentDirectory_whenParentDirectoryIsNotEmpty(TestContext context) throws IOException {
    // given
    Async async = context.async();
    createTestFileAndDirectories(testFile1);
    createAnotherFileInFileDefinitionDirectory();
    fileDefinition1.getMetadata()
      .withUpdatedDate(new Date(new Date().getTime() - ONE_HOUR_ONE_MINUTE_IN_MILLIS));

    // when
    fileDefinitionDao.save(fileDefinition1, TENANT_ID).compose(saveAr -> {
      Future<Boolean> future = storageCleanupService.cleanStorage(okapiConnectionParams);

      // then
      future.setHandler(ar -> {
        assertTrue(ar.succeeded());
        assertTrue(ar.result());
        assertFalse(testFile1.exists());
        assertFileDefinitionIsRemoved();
        async.complete();
      });
      return Promise.promise().future();
    });
  }

  @Test
  public void shouldRemoveFileDefinition_whenFileDoesNotExist_andFileDefinitionWasUpdatedLaterThanHourAgo(TestContext context) {
    // given
    Async async = context.async();
    fileDefinition1.getMetadata()
      .withUpdatedDate(new Date(new Date().getTime() - ONE_HOUR_ONE_MINUTE_IN_MILLIS));

    //when
    fileDefinitionDao.save(fileDefinition1, TENANT_ID).compose(saveAr -> {
      Future<Boolean> future = storageCleanupService.cleanStorage(okapiConnectionParams);

      // then
      future.setHandler(ar -> {
        assertTrue(ar.succeeded());
        assertTrue(ar.result());
        assertFileDefinitionIsRemoved();
        async.complete();
      });
      return Promise.promise().future();
    });
  }

  @Test
  public void shouldNotRemoveFileDefinition_whenSourcePathEmpty_andFileDefinitionWasUpdatedLaterThanHourAgo(TestContext context) {
    // given
    Async async = context.async();
    fileDefinition1.withSourcePath(EMPTY)
      .getMetadata()
      .withUpdatedDate(new Date(new Date().getTime() - ONE_HOUR_ONE_MINUTE_IN_MILLIS));

    // when
    fileDefinitionDao.save(fileDefinition1, TENANT_ID).compose(saveAr -> {
      Future<Boolean> future = storageCleanupService.cleanStorage(okapiConnectionParams);

      // then
      future.setHandler(ar -> {
        assertTrue(ar.succeeded());
        assertFalse(ar.result());
        assertFileDefinitionIsNotRemoved();
        async.complete();
      });
      return Promise.promise().future();
    });
  }

  @Test
  public void shouldNotRemoveFileDefinition_whenSourcePathEmpty_andFileDefinitionWasUpdatedEarlierThanHourAgo(TestContext context) {
    // given
    Async async = context.async();
    fileDefinition1.withSourcePath(EMPTY)
      .getMetadata()
      .withUpdatedDate(new Date(new Date().getTime() - FIFTY_NINE_MINUTES_IN_MILLIS));

    // when
    fileDefinitionDao.save(fileDefinition1, TENANT_ID).compose(saveAr -> {
      Future<Boolean> future = storageCleanupService.cleanStorage(okapiConnectionParams);

      // then
      future.setHandler(ar -> {
        assertTrue(ar.succeeded());
        assertFalse(ar.result());
        assertFileDefinitionIsNotRemoved();
        async.complete();
      });
      return Promise.promise().future();
    });
  }

  private void clearFileDefinitionTable(TestContext context) {
    Async async = context.async();
    PostgresClient.getInstance(vertx, TENANT_ID).delete(FILE_DEFINITIONS_TABLE, new Criterion(), event -> {
      if (event.failed()) {
        context.fail(event.cause());
      }
      async.complete();
    });
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
    fileDefinitionDao.getById(FILE_DEFINITION_ID_1, TENANT_ID).setHandler(fileDefinitionAr -> {
      assertTrue(fileDefinitionAr.failed());
    });
  }

  private void assertFileDefinitionIsNotRemoved() {
    fileDefinitionDao.getById(FILE_DEFINITION_ID_1, TENANT_ID).setHandler(fileDefinitionAr -> {
      assertEquals(fileDefinitionAr.result().get(), fileDefinition1);
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
