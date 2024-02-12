package org.folio.dataexp.repository;

import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class FolioHoldingsAllRepositoryTest extends AllRepositoryTest {

  @Autowired
  private FolioHoldingsAllRepository folioHoldingsAllRepository;

  @Test
  void findFolioHoldingsAllNonDeletedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice = folioHoldingsAllRepository.findFolioHoldingsAllNonDeleted(MIN_UUID, MAX_UUID, PageRequest.of(0, exportIdsBatch));
      assertThat(slice.getContent().size()).isEqualTo(3);
    }
  }

  @Test
  void findFolioHoldingsAllNonDeletedNonSuppressedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice = folioHoldingsAllRepository.findFolioHoldingsAllNonDeletedNonSuppressed(MIN_UUID, MAX_UUID, PageRequest.of(0, exportIdsBatch));
      assertThat(slice.getContent().size()).isEqualTo(2);
    }
  }

  @Test
  void findFolioHoldingsAllDeletedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = folioHoldingsAllRepository.findFolioHoldingsAllDeleted();
      assertThat(list.size()).isEqualTo(3);
    }
  }

  @Test
  void findFolioHoldingsAllDeletedNonSuppressedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = folioHoldingsAllRepository.findFolioHoldingsAllDeletedNonSuppressed();
      assertThat(list.size()).isEqualTo(2);
    }
  }
}
