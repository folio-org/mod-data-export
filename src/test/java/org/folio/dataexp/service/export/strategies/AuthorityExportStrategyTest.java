package org.folio.dataexp.service.export.strategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorityExportStrategyTest {

  @Spy @InjectMocks private AuthorityExportStrategy authorityExportStrategy;

  @Test
  @TestMate(name = "TestMate-647f556456c64f03c802376366b73ea4")
  void getMarcRecordShouldReturnLatestGenerationWhenMultipleRecordsExist() {
    // Given
    var recordId = UUID.fromString("f8b07070-8c9a-4195-9995-c4b1a1c3c3d3");
    var olderRecord = new MarcRecordEntity();
    olderRecord.setExternalId(recordId);
    olderRecord.setState("DELETED");
    olderRecord.setGeneration(1);
    var newerRecord = new MarcRecordEntity();
    newerRecord.setExternalId(recordId);
    newerRecord.setState("ACTUAL");
    newerRecord.setGeneration(2);
    var marcAuthorities = List.of(olderRecord, newerRecord);
    doReturn(marcAuthorities).when(authorityExportStrategy).getMarcAuthorities(Set.of(recordId));
    // When
    var actualRecord = authorityExportStrategy.getMarcRecord(recordId);
    // Then
    assertThat(actualRecord).isEqualTo(newerRecord);
    assertThat(actualRecord.getGeneration()).isEqualTo(2);
    assertThat(actualRecord.getState()).isEqualTo("ACTUAL");
    verify(authorityExportStrategy).getMarcAuthorities(Set.of(recordId));
  }

  @Test
  @TestMate(name = "TestMate-a50aca2d610b4a2bc276d52b20e37d0c")
  void getMarcRecordShouldReturnSingleRecordWhenOneExists() {
    // Given
    var recordId = UUID.fromString("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");
    var marcRecord = new MarcRecordEntity();
    marcRecord.setExternalId(recordId);
    marcRecord.setState("ACTUAL");
    marcRecord.setGeneration(1);
    var marcAuthorities = List.of(marcRecord);
    doReturn(marcAuthorities).when(authorityExportStrategy).getMarcAuthorities(Set.of(recordId));
    // When
    var actualRecord = authorityExportStrategy.getMarcRecord(recordId);
    // Then
    assertThat(actualRecord).isEqualTo(marcRecord);
    verify(authorityExportStrategy).getMarcAuthorities(Set.of(recordId));
  }
}
