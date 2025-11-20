package org.folio.dataexp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.dataexp.BaseDataExportInitializerIT;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DataExportTenantServiceIT extends BaseDataExportInitializerIT {

  @Autowired
  private DataExportTenantService dataExportTenantService;
  @Autowired
  private JobProfileEntityRepository jobProfileEntityRepository;
  @Autowired
  private MappingProfileEntityRepository mappingProfileEntityRepository;

  @Test
  void loadReferenceDataTest() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      dataExportTenantService.loadReferenceData();

      var expectedDefaultJobProfileAmount = 4;
      var expectedDefaultMappingProfileAmount = 3;

      assertEquals(expectedDefaultJobProfileAmount, jobProfileEntityRepository.count());
      assertEquals(expectedDefaultMappingProfileAmount, mappingProfileEntityRepository.count());

      jobProfileEntityRepository.deleteAll();
      mappingProfileEntityRepository.deleteAll();
    }
  }
}
