package org.folio.service.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.List;

@Service
public class FileExportServiceImpl implements FileExportService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void export(List<String> marcRecords) {
    LOGGER.info("Received [{}] marc records on export", marcRecords.size());
  }
}
