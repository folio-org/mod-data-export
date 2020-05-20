package org.folio.service.file.reader;

import org.folio.rest.jaxrs.model.FileDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.UncheckedIOException;

import static org.junit.Assert.assertEquals;

class LocalStorageCsvSourceReaderUnitTest {

  private static final int BATCH_SIZE = 2;
  private static final String NONEXISTING_FILE_NAME = "nonexistingfile";
  private static final String INVENTORY_UUIDS_FILE_NAME = "src/test/resources/files/InventoryUUIDs.csv";
  private static final long TOTAL_COUNT_5 = 5L;
  private static final long TOTAL_COUNT_0 = 0L;

  private static LocalStorageCsvSourceReader reader;

  @BeforeAll
  public static void setUp() {
    reader = new LocalStorageCsvSourceReader();
  }

  @Test
  void shouldReturnTotalCountZero_whenReaderIsNotInitialized() {
    //when
    long actualTotalCount = reader.totalCount();
    //then
    assertEquals(TOTAL_COUNT_0, actualTotalCount);
  }

  @Test
  void shouldReturnTotalCountZero_whenReaderInitializedWithNonExistingFile() {
    FileDefinition fileDefinition = new FileDefinition()
      .withSourcePath(NONEXISTING_FILE_NAME);
    //when
    reader.init(fileDefinition, BATCH_SIZE);
    long actualTotalCount = reader.totalCount();
    //then
    assertEquals(TOTAL_COUNT_0, actualTotalCount);
  }

  @Test
  void shouldReturnTotalCountFive_whenReaderInitialized() {
    //given
    FileDefinition fileDefinition = new FileDefinition()
      .withSourcePath(INVENTORY_UUIDS_FILE_NAME);
    //when
    reader.init(fileDefinition, BATCH_SIZE);
    long actualTotalCount = reader.totalCount();
    //then
    assertEquals(TOTAL_COUNT_5, actualTotalCount);
  }

  @Test
  void shouldNotThrowException_whenCloseNotInitializedReader() {
    reader.close();
  }

  @Test
  void shouldThrowUncheckedIOException_whenReadNextAfterClose() {
    //given
    FileDefinition fileDefinition = new FileDefinition()
      .withSourcePath(INVENTORY_UUIDS_FILE_NAME);
    //when
    reader.init(fileDefinition, BATCH_SIZE);
    reader.close();
    //then
    Assertions.assertThrows(UncheckedIOException.class, () -> {
      reader.readNext();
    });

  }

}
