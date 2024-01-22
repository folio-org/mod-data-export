package org.folio.dataexp.repository;

import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

public class HoldingsRecordAllEntityRepositoryTest extends BaseDataExportInitializer {

//  @Autowired
//  private HoldingsRecordAllEntityRepository holdingsRecordAllEntityRepository;
//
//  @Test
//  void test() {
//    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
//      HoldingsRecordAllEntity entity = new HoldingsRecordAllEntity();
//      entity.setId(UUID.randomUUID());
//      entity.setJsonb("");
//      entity.setInstanceId(UUID.fromString("011e1aea-222d-4d1d-957d-0abcdd0e9acd"));
//      holdingsRecordAllEntityRepository.save(entity);
//    }
//  }
}
