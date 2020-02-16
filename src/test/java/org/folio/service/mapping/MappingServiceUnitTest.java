package org.folio.service.mapping;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class MappingServiceUnitTest {

  @Test(expected = UnsupportedOperationException.class)
  public void shouldThrowException() {
    MappingService mappingService = new MappingServiceImpl();
    mappingService.map(new ArrayList<>());
  }
}
