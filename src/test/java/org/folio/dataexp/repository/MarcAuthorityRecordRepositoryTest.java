package org.folio.dataexp.repository;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.persistence.EntityManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class MarcAuthorityRecordRepositoryTest {

  @Mock
  private EntityManager entityManager;

  @InjectMocks
  private MarcAuthorityRecordRepository marcAuthorityRecordRepository;

    @Mock
private Query query;

    @Test
void findNonDeletedByExternalIdInShouldReturnMappedEntitiesWhenRecordsExist() {
  // TestMate-072380faa04e785431ec6568d00474ae
  // Given
  var tenantId = "diku";
  var id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
  var id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
  var ids = Set.of(id1, id2);
  var entity1 = MarcRecordEntity.builder()
      .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
      .externalId(id1)
      .state("ACTUAL")
      .build();
  var entity2 = MarcRecordEntity.builder()
      .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
      .externalId(id2)
      .state("DELETED")
      .build();
  var expectedEntities = List.of(entity1, entity2);
  var sqlCaptor = ArgumentCaptor.forClass(String.class);
  when(entityManager.createNativeQuery(sqlCaptor.capture(), eq(MarcRecordEntity.class))).thenReturn(query);
  when(query.getResultList()).thenReturn(expectedEntities);
  // When
  var actualEntities = marcAuthorityRecordRepository.findNonDeletedByExternalIdIn(tenantId, ids);
  // Then
  assertThat(actualEntities).hasSize(2).containsExactlyElementsOf(expectedEntities);
  assertThat(sqlCaptor.getValue()).contains("diku_mod_source_record_storage");
  verify(query).setParameter("ids", ids);
}

}
