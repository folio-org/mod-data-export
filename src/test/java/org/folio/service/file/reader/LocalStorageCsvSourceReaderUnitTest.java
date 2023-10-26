package org.folio.service.file.reader;

import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.manager.export.ExportPayload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

class LocalStorageCsvSourceReaderUnitTest {

  private static final int BATCH_SIZE = 2;
  private static final String NONEXISTING_FILE_NAME = "nonexistingfile";
  private static final String INVENTORY_UUIDS_FILE_NAME = "src/test/resources/files/InventoryUUIDs.csv";
  private static final String INVENTORY_UUIDS_WITH_WRONG_FORMATS = "src/test/resources/files/InventoryUUIDsWithInvalidUUIDs.csv";
  private static final String INVENTORY_INVALID_FILE_CONTENT = "src/test/resources/files/InventoryInvalidFileContent.csv";
  private static final String UUID_PATTERN = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$";
  private static final long TOTAL_COUNT_5 = 5L;
  private static final long TOTAL_COUNT_3 = 3L;
  private static final long TOTAL_COUNT_0 = 0L;

  private static LocalStorageCsvSourceReader reader;
  private static ErrorLogService errorLogService;
  private static String jobExecutionId;
  private static String tenantId;

  @BeforeAll
  public static void setUp() {
    reader = new LocalStorageCsvSourceReader();
    errorLogService = Mockito.mock(ErrorLogService.class);
    jobExecutionId = UUID.randomUUID().toString();
    tenantId = UUID.randomUUID().toString();
  }

  @Test
  void shouldReturnTotalCountZero_whenReaderIsNotInitialized() {
    //when
    long actualTotalCount = reader.totalCount(new ExportPayload());
    //then
    assertEquals(TOTAL_COUNT_0, actualTotalCount);
  }

  @Test
  void shouldReturnTotalCountZero_whenReaderInitializedWithNonExistingFile() {
    FileDefinition fileDefinition = new FileDefinition()
      .withSourcePath(NONEXISTING_FILE_NAME);
    //when
    reader.init(fileDefinition, errorLogService, jobExecutionId, tenantId, BATCH_SIZE);
    long actualTotalCount = reader.totalCount(new ExportPayload());
    //then
    assertEquals(TOTAL_COUNT_0, actualTotalCount);
  }

  @Test
  void shouldReturnTotalCountFive_whenReaderInitialized() {
    //given
    FileDefinition fileDefinition = new FileDefinition()
      .withSourcePath(INVENTORY_UUIDS_FILE_NAME);
    //when
    reader.init(fileDefinition, errorLogService, jobExecutionId, tenantId, BATCH_SIZE);
    long actualTotalCount = reader.totalCount(new ExportPayload());
    //then
    assertEquals(TOTAL_COUNT_5, actualTotalCount);
  }

  @Test
  void shouldReturnTotalCountThree_whenReaderInitialized_AndSkipInvalidFields() {
    //given
    FileDefinition fileDefinition = new FileDefinition()
      .withSourcePath(INVENTORY_UUIDS_WITH_WRONG_FORMATS);
    ErrorLogService errorLogService = Mockito.mock(ErrorLogService.class);
    //when
    reader.init(fileDefinition, errorLogService, jobExecutionId, tenantId, 50);
    List<String> uuidList = reader.readNext();
    long actualTotalCount = reader.totalCount(new ExportPayload());
    //then
    for (String uuid : uuidList) {
      Assertions.assertFalse(uuid.isEmpty());
      Assertions.assertTrue(uuid.matches(UUID_PATTERN));
      assertFalse(uuid.contains(","));
      assertFalse(uuid.contains("\""));
    }
    assertEquals(TOTAL_COUNT_3, actualTotalCount);
    Mockito.verify(errorLogService).saveGeneralErrorWithMessageValues(anyString(), anyList(),anyString(), anyString());
  }

  @Test
  void shouldNotThrowException_whenCloseNotInitializedReader() {
    assertDoesNotThrow(() -> reader.close());
  }

  @Test
  void shouldThrowUncheckedIOException_whenReadNextAfterClose() {
    //given
    FileDefinition fileDefinition = new FileDefinition()
      .withSourcePath(INVENTORY_UUIDS_FILE_NAME);
    //when
    reader.init(fileDefinition, errorLogService, jobExecutionId, tenantId, BATCH_SIZE);
    reader.close();
    //then
    Assertions.assertThrows(UncheckedIOException.class, () -> {
      reader.readNext();
    });

  }

  @Test
  void shouldReturnFalseInHasNext_whenInvalidFileContent() {
    //given
    FileDefinition fileDefinition = new FileDefinition()
      .withSourcePath(INVENTORY_INVALID_FILE_CONTENT);
    //when
    reader.init(fileDefinition, errorLogService, jobExecutionId, tenantId, BATCH_SIZE);
    reader.close();
    //then
    assertFalse(reader.hasNext());
  }

}
