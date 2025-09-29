package org.folio.dataexp.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class MarcInstanceAllRepositoryTest extends AllRepositoryTest {

  @Autowired
  private MarcInstanceAllRepository marcInstanceAllRepository;

  @Test
  void findMarcInstanceAllNonDeletedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice = marcInstanceAllRepository.findMarcInstanceAllNonDeleted(MIN_UUID, MAX_UUID,
          PageRequest.of(0, exportIdsBatch));
      assertThat(slice.getContent()).hasSize(5);
    }
  }

  @Test
  void findMarcInstanceAllNonDeletedNonSuppressedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice = marcInstanceAllRepository.findMarcInstanceAllNonDeletedNonSuppressed(MIN_UUID,
          MAX_UUID, PageRequest.of(0, exportIdsBatch));
      assertThat(slice.getContent()).hasSize(3);
    }
  }

  @Test
  void findMarcInstanceAllDeletedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = marcInstanceAllRepository.findMarcInstanceAllDeleted();
      assertThat(list).hasSize(12);
    }
  }

  @Test
  void findMarcInstanceAllDeletedNonSuppressedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = marcInstanceAllRepository.findMarcInstanceAllDeletedNonSuppressed();
      assertThat(list).hasSize(6);
    }
  }
}
