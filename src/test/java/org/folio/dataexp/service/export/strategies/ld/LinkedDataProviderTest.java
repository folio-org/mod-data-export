package org.folio.dataexp.service.export.strategies.ld;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.client.QueryClient;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class LinkedDataProviderTest extends BaseDataExportInitializer {

  @MockitoBean
  private QueryClient queryClient;

  @Autowired
  private LinkedDataProvider linkedDataProvider;

  @Test
  void getLinkedDataResourcesTest() {
    var id1 = UUID.randomUUID();
    var id2 = UUID.randomUUID();
    when(queryClient.getContents(any(ContentsRequest.class))).thenReturn(
        List.of(
          Map.of("inventory_id", id1, "resource_subgraph", id1.toString()),
          Map.of("inventory_id", id2, "resource_subgraph", id2.toString())
        )
    );
    var resources = linkedDataProvider.getLinkedDataResources(Set.of(id1, id2));
    assertEquals(2, resources.size());
    assertEquals(id1.toString(), resources.get(0));
    assertEquals(id2.toString(), resources.get(1));
  }
}
