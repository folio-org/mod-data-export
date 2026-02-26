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
}
