package org.folio.service.export;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileExportServiceImpl implements FileExportService {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileExportServiceImpl.class);

  @Override
  public void export(List<String> marcRecords) {
    LOGGER.info("Received [" + marcRecords.size() + "] on export");
  }
}
