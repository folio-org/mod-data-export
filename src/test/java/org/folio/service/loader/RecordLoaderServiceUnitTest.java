package org.folio.service.loader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class RecordLoaderServiceUnitTest {

  @Test(expected = UnsupportedOperationException.class)
  public void shouldThrowException_WhenLoadMarc() {
    RecordLoaderService recordLoaderService = new RecordLoaderServiceImpl();
    recordLoaderService.loadMarcByInstanceIds(new ArrayList<>());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldThrowException_WhenLoadInstances() {
    RecordLoaderService recordLoaderService = new RecordLoaderServiceImpl();
    recordLoaderService.loadInstancesByIds(new ArrayList<>());
  }
}
