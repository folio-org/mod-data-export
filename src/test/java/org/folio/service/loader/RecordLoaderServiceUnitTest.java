package org.folio.service.loader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThatCode;

@RunWith(MockitoJUnitRunner.class)
public class RecordLoaderServiceUnitTest {

  @Test
  public void loadMarcRecords_doesNotThrowAnyException() {
    RecordLoaderService recordLoaderService = new RecordLoaderServiceImpl();
    assertThatCode(() -> recordLoaderService.loadSrsMarcRecords(new ArrayList<>())).doesNotThrowAnyException();
  }

  @Test
  public void loadInstanceRecords_doesNotThrowAnyException() {
    RecordLoaderService recordLoaderService = new RecordLoaderServiceImpl();
    recordLoaderService.loadInventoryInstances(new ArrayList<>());
    assertThatCode(() -> recordLoaderService.loadInventoryInstances(new ArrayList<>())).doesNotThrowAnyException();
  }
}
