package org.folio.dataexp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.client.QueryClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.Field;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

  @Mock
  private QueryClient queryClient;

  @InjectMocks
  private QueryService queryService;

    @Test
  void getEntitiesShouldReturnListOfEntitiesWhenGivenValidInputs() {
    // TestMate-e3c263012387e62d7dbcd98974adfc10
    // Given
    var id1 = UUID.fromString("c2755022-9594-4a2c-843a-733b499ad26d");
    var id2 = UUID.fromString("f4a1b2c3-d4e5-f6a7-b8c9-d0e1f2a3b4c5");
    var ids = Set.of(id1, id2);
    var entityTypeId = UUID.fromString("a1b2c3d4-e5f6-a7b8-c9d0-e1f2a3b4c5d6");
    var fields = List.of("id", "name");
    List<Map<String, Object>> expectedEntities = List.of(
      Map.of("id", "c2755022-9594-4a2c-843a-733b499ad26d", "name", "Entity One"),
      Map.of("id", "f4a1b2c3-d4e5-f6a7-b8c9-d0e1f2a3b4c5", "name", "Entity Two")
    );
    when(queryClient.getContents(any(ContentsRequest.class))).thenReturn(expectedEntities);
    // When
    var actualEntities = queryService.getEntities(ids, entityTypeId, fields);
    // Then
    assertEquals(expectedEntities, actualEntities);
    verify(queryClient, times(1)).getContents(any(ContentsRequest.class));
  }
}
