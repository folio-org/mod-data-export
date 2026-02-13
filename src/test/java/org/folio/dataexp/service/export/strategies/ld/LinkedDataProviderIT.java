package org.folio.dataexp.service.export.strategies.ld;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.folio.dataexp.BaseDataExportInitializerIT;
import org.folio.dataexp.client.QueryClient;
import org.folio.dataexp.domain.dto.LinkedDataResource;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class LinkedDataProviderIT extends BaseDataExportInitializerIT {

  @MockitoBean private QueryClient queryClient;

  @Autowired private LinkedDataProvider linkedDataProvider;

  @ParameterizedTest
  @MethodSource("providedResources")
  void getLinkedDataResourcesTest(List<TestResource> resources) {
    when(queryClient.getContents(any(ContentsRequest.class)))
        .thenReturn(resources.stream().map(TestResource::getResource).toList());
    var validCount = resources.stream().filter(r -> r.isValid()).count();
    var expected = resources.stream().filter(r -> r.isValid()).map(TestResource::toDto).toList();

    var results =
        linkedDataProvider.getLinkedDataResources(
            resources.stream().map(TestResource::getId).collect(Collectors.toSet()));

    assertEquals(validCount, results.size());
    assertTrue(results.containsAll(expected));
    assertTrue(expected.containsAll(results));
  }

  @Getter
  @Setter
  @NoArgsConstructor
  private static class TestResource {
    UUID id;
    boolean valid;
    Map<String, Object> resource;

    public LinkedDataResource toDto() {
      var dto = new LinkedDataResource();
      dto.setInventoryId(id.toString());
      dto.setResource(id.toString());
      return dto;
    }
  }

  private static List<TestResource> generateResources(int count, int validCount) {
    var list = new ArrayList<TestResource>(count);
    for (var i = 0; i < count; i++) {
      var id = UUID.randomUUID();
      var resource = new TestResource();
      var column = new LinkedHashMap<String, String>();
      column.put("type", "jsonb");
      column.put("value", id.toString());
      resource.setId(id);
      resource.setValid(i < validCount);
      if (i < validCount) {
        resource.setResource(Map.of("inventory_id", id.toString(), "resource_subgraph", column));
      } else {
        resource.setResource(Map.of("inventory_id", id.toString(), "not_the_right_field", column));
      }
      list.add(i, resource);
    }
    return list;
  }

  private static Stream<Arguments> providedResources() {
    return Stream.of(
        Arguments.of(generateResources(3, 3)),
        Arguments.of(generateResources(3, 2)),
        Arguments.of(generateResources(3, 0)));
  }
}
