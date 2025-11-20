package org.folio.dataexp.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class MarcHoldingsAllRepositoryIT extends AllRepositoryBaseIT {

  @Autowired
  private MarcHoldingsAllRepository marcHoldingsAllRepository;

  @Test
  void findMarcHoldingsAllNonDeletedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice = marcHoldingsAllRepository.findMarcHoldingsAllNonDeleted(MIN_UUID, MAX_UUID,
          PageRequest.of(0, exportIdsBatch));
      assertThat(slice.getContent()).hasSize(6);
    }
  }

  @Test
  void findMarcHoldingsAllNonDeletedNonSuppressedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice = marcHoldingsAllRepository.findMarcHoldingsAllNonDeletedNonSuppressed(MIN_UUID,
          MAX_UUID, PageRequest.of(0, exportIdsBatch));
      assertThat(slice.getContent()).hasSize(4);
    }
  }

  @Test
  void findMarcHoldingsAllDeletedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = marcHoldingsAllRepository.findMarcHoldingsAllDeleted();
      assertThat(list).hasSize(6);
    }
  }

  @Test
  void findMarcHoldingsAllDeletedNonSuppressedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = marcHoldingsAllRepository.findMarcHoldingsAllDeletedNonSuppressed();
      assertThat(list).hasSize(3);
    }
  }
}
