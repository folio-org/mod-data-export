package org.folio.service.file.reader;

import org.folio.rest.jaxrs.model.FileDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.UncheckedIOException;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@RunWith(MockitoJUnitRunner.class)
public class LocalStorageCsvSourceReaderUnitTest {

  private static final int BATCH_SIZE = 2;
  private static final String NONEXISTING_FILE_NAME = "nonexistingfile";
  private static final String INVENTORY_UUIDS_FILE_NAME = "src/test/resources/files/InventoryUUIDs.csv";
  private static final long TOTAL_COUNT_5 = 5L;
  private static final long TOTAL_COUNT_0 = 0L;

  private LocalStorageCsvSourceReader reader;

  @Before
  public void setUp() {
    reader = new LocalStorageCsvSourceReader();
  }

  @Test
  public void shouldReturnTotalCountZero_whenReaderIsNotInitialized() {
    //when
    long actualTotalCount = reader.totalCount();
    //then
    assertEquals(TOTAL_COUNT_0, actualTotalCount);
  }

  @Test
  public void shouldReturnTotalCountZero_whenReaderInitializedWithNonExistingFile() {
    FileDefinition fileDefinition = new FileDefinition()
      .withSourcePath(NONEXISTING_FILE_NAME);
    //when
    reader.init(fileDefinition, BATCH_SIZE);
    long actualTotalCount = reader.totalCount();
    //then
    assertEquals(TOTAL_COUNT_0, actualTotalCount);
  }

  @Test
  public void shouldReturnTotalCountFive_whenReaderInitialized() {
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
  public void shouldNotThrowException_whenCloseNotInitializedReader() {
    assertDoesNotThrow(() -> reader.close());
  }

  @Test(expected = UncheckedIOException.class)
  public void shouldThrowUncheckedIOException_whenReadNextAfterClose() {
    //given
    FileDefinition fileDefinition = new FileDefinition()
      .withSourcePath(INVENTORY_UUIDS_FILE_NAME);
    //when
    reader.init(fileDefinition, BATCH_SIZE);
    reader.close();
    reader.readNext();
  }

}
