package org.folio.dataexp.service.export.strategies;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.MarcAuthorityRecordAllRepository;
import org.folio.dataexp.service.export.LocalStorageWriter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class AuthorityExportAllStrategyTest {

    @Mock
private MarcAuthorityRecordAllRepository marcAuthorityRecordAllRepository;

    @Mock
private LocalStorageWriter localStorageWriter;

    @Mock
private ExportStrategyStatistic exportStatistic;

    @Spy
@InjectMocks
private AuthorityExportAllStrategy authorityExportAllStrategy;

    @Captor
private ArgumentCaptor<Set<UUID>> exportIdsCaptor;

    @Captor
private ArgumentCaptor<List<MarcRecordEntity>> marcRecordsCaptor;

    @Captor
private ArgumentCaptor<Pageable> pageableCaptor;

    @Test
void processSlicesShouldExportSingleSliceWithoutDeletedRecords() {
    // TestMate-a296f59045cc846a0e9f7b590af4a637
    // Given
    var jobExecutionId = UUID.fromString("a892033a-3366-4b53-af27-1f3b2843511e");
    var fromId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var toId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    var exportRequest = new ExportRequest().deletedRecords(false);
    var exportFilesEntity = new JobExecutionExportFilesEntity()
        .withJobExecutionId(jobExecutionId)
        .withFromId(fromId)
        .withToId(toId);
    var mappingProfile = new MappingProfile();
    var marcRecord = MarcRecordEntity.builder()
        .id(UUID.fromString("111841e2-0402-4344-8557-1523fdf2f73e"))
        .externalId(UUID.fromString("221841e2-0402-4344-8557-1523fdf2f73f"))
        .build();
    List<MarcRecordEntity> marcRecords = List.of(marcRecord);
    var pageable = PageRequest.of(0, 1);
    Slice<MarcRecordEntity> slice = new SliceImpl<>(marcRecords, pageable, false);
    authorityExportAllStrategy.setExportIdsBatch(1);
    // The field marcAuthorityRecordAllRepository is in the parent class and not injected by @InjectMocks
    // So it needs to be set manually.
    authorityExportAllStrategy.marcAuthorityRecordAllRepository = marcAuthorityRecordAllRepository;
    when(marcAuthorityRecordAllRepository.findAllWithoutDeleted(any(UUID.class), any(UUID.class), any(Pageable.class)))
        .thenReturn(slice);
    doNothing().when(authorityExportAllStrategy).createAndSaveMarc(anySet(), anyList(), any(ExportStrategyStatistic.class), any(MappingProfile.class), any(UUID.class), any(LocalStorageWriter.class));
    // When
    authorityExportAllStrategy.processSlices(exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    // Then
    verify(marcAuthorityRecordAllRepository).findAllWithoutDeleted(fromId, toId, pageable);
    verify(marcAuthorityRecordAllRepository, never()).findAllWithDeleted(any(), any(), any());
    verify(authorityExportAllStrategy).createAndSaveMarc(exportIdsCaptor.capture(), marcRecordsCaptor.capture(),
        any(ExportStrategyStatistic.class), any(MappingProfile.class), any(UUID.class), any(LocalStorageWriter.class));
    Set<UUID> expectedIds = marcRecords.stream().map(MarcRecordEntity::getExternalId).collect(Collectors.toSet());
    assertThat(exportIdsCaptor.getValue()).containsExactlyInAnyOrderElementsOf(expectedIds);
    assertThat(marcRecordsCaptor.getValue()).isEqualTo(marcRecords);
}

    @Test
void processSlicesShouldProcessMultipleSlicesUntilExhausted() {
    // TestMate-075f4ef63c0f392369e42f22a9ac3fc1
    // Given
    var jobExecutionId = UUID.fromString("a892033a-3366-4b53-af27-1f3b2843511e");
    var fromId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var toId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    var exportRequest = new ExportRequest().deletedRecords(false);
    var exportFilesEntity = new JobExecutionExportFilesEntity()
      .withJobExecutionId(jobExecutionId)
      .withFromId(fromId)
      .withToId(toId);
    var mappingProfile = new MappingProfile();
    var marcRecord1 = MarcRecordEntity.builder().id(UUID.randomUUID()).externalId(UUID.randomUUID()).build();
    var marcRecord2 = MarcRecordEntity.builder().id(UUID.randomUUID()).externalId(UUID.randomUUID()).build();
    var marcRecord3 = MarcRecordEntity.builder().id(UUID.randomUUID()).externalId(UUID.randomUUID()).build();
    var pageable1 = PageRequest.of(0, 1);
    var pageable2 = PageRequest.of(1, 1);
    var pageable3 = PageRequest.of(2, 1);
    Slice<MarcRecordEntity> slice1 = new SliceImpl<>(List.of(marcRecord1), pageable1, true);
    Slice<MarcRecordEntity> slice2 = new SliceImpl<>(List.of(marcRecord2), pageable2, true);
    Slice<MarcRecordEntity> slice3 = new SliceImpl<>(List.of(marcRecord3), pageable3, false);
    authorityExportAllStrategy.setExportIdsBatch(1);
    authorityExportAllStrategy.marcAuthorityRecordAllRepository = marcAuthorityRecordAllRepository;
    when(marcAuthorityRecordAllRepository.findAllWithoutDeleted(fromId, toId, pageable1)).thenReturn(slice1);
    when(marcAuthorityRecordAllRepository.findAllWithoutDeleted(fromId, toId, pageable2)).thenReturn(slice2);
    when(marcAuthorityRecordAllRepository.findAllWithoutDeleted(fromId, toId, pageable3)).thenReturn(slice3);
    doNothing().when(authorityExportAllStrategy).createAndSaveMarc(anySet(), anyList(), any(ExportStrategyStatistic.class), any(MappingProfile.class), any(UUID.class), any(LocalStorageWriter.class));
    // When
    authorityExportAllStrategy.processSlices(exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    // Then
    verify(marcAuthorityRecordAllRepository, times(3)).findAllWithoutDeleted(any(UUID.class), any(UUID.class), pageableCaptor.capture());
    verify(marcAuthorityRecordAllRepository, never()).findAllWithDeleted(any(), any(), any());
    List<Pageable> capturedPageables = pageableCaptor.getAllValues();
    assertThat(capturedPageables).containsExactly(pageable1, pageable2, pageable3);
    verify(authorityExportAllStrategy, times(3)).createAndSaveMarc(exportIdsCaptor.capture(), marcRecordsCaptor.capture(),
      any(ExportStrategyStatistic.class), any(MappingProfile.class), any(UUID.class), any(LocalStorageWriter.class));
    List<Set<UUID>> allExportIds = exportIdsCaptor.getAllValues();
    assertThat(allExportIds.get(0)).containsExactly(marcRecord1.getExternalId());
    assertThat(allExportIds.get(1)).containsExactly(marcRecord2.getExternalId());
    assertThat(allExportIds.get(2)).containsExactly(marcRecord3.getExternalId());
    List<List<MarcRecordEntity>> allMarcRecords = marcRecordsCaptor.getAllValues();
    assertThat(allMarcRecords.get(0)).containsExactly(marcRecord1);
    assertThat(allMarcRecords.get(1)).containsExactly(marcRecord2);
    assertThat(allMarcRecords.get(2)).containsExactly(marcRecord3);
}

    @Test
    void processSlicesShouldHandleEmptyFirstSlice() {
        // TestMate-88b944c1c16eb4fa35a0b1af4df86cda
        // Given
        var jobExecutionId = UUID.fromString("a892033a-3366-4b53-af27-1f3b2843511e");
        var fromId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var toId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        var exportRequest = new ExportRequest().deletedRecords(false);
        var exportFilesEntity = new JobExecutionExportFilesEntity()
            .withJobExecutionId(jobExecutionId)
            .withFromId(fromId)
            .withToId(toId);
        var mappingProfile = new MappingProfile();
        var pageable = PageRequest.of(0, 1);
        Slice<MarcRecordEntity> emptySlice = new SliceImpl<>(Collections.emptyList(), pageable, false);
        authorityExportAllStrategy.setExportIdsBatch(1);
        authorityExportAllStrategy.marcAuthorityRecordAllRepository = marcAuthorityRecordAllRepository;
        when(marcAuthorityRecordAllRepository.findAllWithoutDeleted(fromId, toId, pageable)).thenReturn(emptySlice);
        doNothing().when(authorityExportAllStrategy).createAndSaveMarc(anySet(), anyList(), any(ExportStrategyStatistic.class), any(MappingProfile.class), any(UUID.class), any(LocalStorageWriter.class));
        // When
        authorityExportAllStrategy.processSlices(exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
        // Then
        verify(marcAuthorityRecordAllRepository).findAllWithoutDeleted(fromId, toId, pageable);
        verify(marcAuthorityRecordAllRepository, never()).findAllWithDeleted(any(), any(), any());
        verify(authorityExportAllStrategy).createAndSaveMarc(exportIdsCaptor.capture(), marcRecordsCaptor.capture(),
            any(ExportStrategyStatistic.class), any(MappingProfile.class), any(UUID.class), any(LocalStorageWriter.class));
        assertThat(exportIdsCaptor.getValue()).isEmpty();
        assertThat(marcRecordsCaptor.getValue()).isEmpty();
    }
}
