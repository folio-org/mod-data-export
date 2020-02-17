package org.folio.service.export;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class FileExportServiceUnitTest {

  @Test
  public void save_doesNotThrowAnyException() {
    FileExportService fileExportService = new FileExportServiceImpl();
    Assertions.assertThatCode(() -> fileExportService.save(new ArrayList<>())).doesNotThrowAnyException();
  }
}
