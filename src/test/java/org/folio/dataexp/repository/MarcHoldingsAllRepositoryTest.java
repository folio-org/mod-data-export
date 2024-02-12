package org.folio.dataexp.repository;

import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class MarcHoldingsAllRepositoryTest extends AllRepositoryTest {

  @Autowired
  private MarcHoldingsAllRepository marcHoldingsAllRepository;

  @Test
  void findMarcHoldingsAllNonDeletedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice = marcHoldingsAllRepository.findMarcHoldingsAllNonDeleted(MIN_UUID, MAX_UUID, PageRequest.of(0, exportIdsBatch));
      assertThat(slice.getContent()).hasSize(3);
    }
  }

  @Test
  void findMarcHoldingsAllNonDeletedNonSuppressedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice = marcHoldingsAllRepository.findMarcHoldingsAllNonDeletedNonSuppressed(MIN_UUID, MAX_UUID, PageRequest.of(0, exportIdsBatch));
      assertThat(slice.getContent()).hasSize(2);
    }
  }

  @Test
  void findMarcHoldingsAllDeletedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = marcHoldingsAllRepository.findMarcHoldingsAllDeleted();
      assertThat(list).hasSize(9);
    }
  }

  @Test
  void findMarcHoldingsAllDeletedNonSuppressedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = marcHoldingsAllRepository.findMarcHoldingsAllDeletedNonSuppressed();
      assertThat(list).hasSize(5);
    }
  }
}
