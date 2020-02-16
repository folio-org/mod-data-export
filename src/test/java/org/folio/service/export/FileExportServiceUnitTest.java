package org.folio.service.export;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class FileExportServiceUnitTest {

  @Test(expected = UnsupportedOperationException.class)
  public void shouldThrowException() {
    FileExportService fileExportService = new FileExportServiceImpl();
    fileExportService.save(new ArrayList<>());
  }
}
