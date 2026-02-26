package org.folio.dataexp.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class InsertExportIdServiceTest {

  @Mock private ExportIdEntityRepository exportIdEntityRepository;
  @InjectMocks private InsertExportIdService insertExportIdService;

  @Test
  @TestMate(name = "TestMate-c8bc8d6138c4e7295ddc24f09f4c7d78")
  void saveBatchShouldNotCallInsertForEmptyList() {
    // Given
    List<ExportIdEntity> emptyList = Collections.emptyList();
    // When
    insertExportIdService.saveBatch(emptyList);
    // Then
    verify(exportIdEntityRepository, never()).insertExportId(any(UUID.class), any(UUID.class));
  }

    @Test
  void saveBatchShouldCallInsertForEachEntityInList() {
    // TestMate-6eff45e9658ac662f28c81613fc61e9c
    // Given
    var jobExecutionId1 = UUID.fromString("a1b2c3d4-1111-2222-3333-a1b2c3d4e5f6");
    var instanceId1 = UUID.fromString("f6e5d4c3-2222-1111-a1b2-f6e5d4c3b2a1");
    var jobExecutionId2 = UUID.fromString("b2c3d4e5-4444-5555-6666-b2c3d4e5f6a1");
    var instanceId2 = UUID.fromString("a1b2c3d4-5555-4444-b2c3-a1b2c3d4e5f6");
    var entity1 = ExportIdEntity.builder()
        .jobExecutionId(jobExecutionId1)
        .instanceId(instanceId1)
        .build();
    var entity2 = ExportIdEntity.builder()
        .jobExecutionId(jobExecutionId2)
        .instanceId(instanceId2)
        .build();
    var exportIds = List.of(entity1, entity2);
    // When
    insertExportIdService.saveBatch(exportIds);
    // Then
    verify(exportIdEntityRepository, times(2)).insertExportId(any(UUID.class), any(UUID.class));
    verify(exportIdEntityRepository).insertExportId(jobExecutionId1, instanceId1);
    verify(exportIdEntityRepository).insertExportId(jobExecutionId2, instanceId2);
  }
}
