package org.folio.dataexp.service.export.strategies;

import org.folio.dataexp.domain.entity.AuditInstanceEntity;
import org.folio.dataexp.repository.AuditInstanceEntityRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstancesExportAllStrategyTest {

  @Mock
  private AuditInstanceEntityRepository auditInstanceEntityRepository;
  @Mock
  private InstanceEntityRepository instanceEntityRepository;

  @InjectMocks
  private InstancesExportAllStrategy instancesExportAllStrategy;

  @Test
  void getIdentifierMessageTest() {
    var auditInstanceEntity = AuditInstanceEntity.builder()
      .id(UUID.randomUUID()).hrid("123").title("title").build();

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of());
    when(auditInstanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(auditInstanceEntity));

    var opt = instancesExportAllStrategy.getIdentifiers(UUID.randomUUID());

    assertTrue(opt.isPresent());
    assertEquals("Instance with HRID : 123", opt.get().getIdentifierHridMessage());

    assertEquals(auditInstanceEntity.getId().toString(), opt.get().getAssociatedJsonObject().getAsString("id"));
    assertEquals("title", opt.get().getAssociatedJsonObject().getAsString("title"));
    assertEquals("123", opt.get().getAssociatedJsonObject().getAsString("hrid"));
  }

  @Test
  void getIdentifierMessageIfInstanceDoesNotExistTest() {
    var instanceId  = UUID.fromString("b9d26945-9757-4855-ae6e-fd5d2f7d778e");
    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of());
    when(auditInstanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of());

    var opt = instancesExportAllStrategy.getIdentifiers(instanceId);

    assertTrue(opt.isPresent());
    assertEquals("Instance with ID : b9d26945-9757-4855-ae6e-fd5d2f7d778e", opt.get().getIdentifierHridMessage());
  }
}
