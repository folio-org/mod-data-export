package org.folio.dataexp.service.export.strategies;

import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.MarcInstanceRecordRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InstancesExportStrategyTest {

  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private MarcInstanceRecordRepository marcInstanceRecordRepository;
  @Mock
  private MarcRecordEntityRepository marcRecordEntityRepository;
  @Mock
  private InstanceEntityRepository instanceEntityRepository;

  @InjectMocks
  private InstancesExportStrategy instancesExportStrategy;

  @Test
  void getMarcRecordsTest() {
    var mappingProfile =  new MappingProfile();
    mappingProfile.setDefault(true);

    var record = MarcRecordEntity.builder().externalId(UUID.randomUUID()).build();
    var recordFromCentralTenant = MarcRecordEntity.builder().externalId(UUID.randomUUID()).build();
    var ids = Set.of(record.getExternalId(), recordFromCentralTenant.getExternalId());

    when(marcRecordEntityRepository.findByExternalIdInAndRecordTypeIs(anySet(), anyString())).thenReturn(new ArrayList<>(List.of(record)));
    when(consortiaService.getCentralTenantId()).thenReturn("central");
    when(marcInstanceRecordRepository.findByExternalIdIn(eq("central"), anySet())).thenReturn(new ArrayList<>(List.of(recordFromCentralTenant)));

    var actualMarcRecords = instancesExportStrategy.getMarcRecords(new HashSet<>(ids), mappingProfile);
    assertEquals(2, actualMarcRecords.size());
  }

  @Test
  void getIdentifierMessageTest() {
    var instance = "{'hrid' : '123'}";
    var instanceRecordEntity = InstanceEntity.builder().jsonb(instance).id(UUID.randomUUID()).build();

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceRecordEntity));

    var opt = instancesExportStrategy.getIdentifierMessage(UUID.randomUUID());

    assertTrue(opt.isPresent());
    assertEquals("Instance with hrid : 123", opt.get());
  }
}
