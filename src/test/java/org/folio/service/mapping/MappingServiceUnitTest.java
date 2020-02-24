package org.folio.service.mapping;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThatCode;

@RunWith(MockitoJUnitRunner.class)
public class MappingServiceUnitTest {

  @Test
  public void map_doesNotThrowAnyException() {
    MappingService mappingService = new MappingServiceImpl();
    assertThatCode(() -> mappingService.map(new ArrayList<>())).doesNotThrowAnyException();
  }
}
