package org.folio.dataexp.repository;

import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class FolioInstanceAllRepositoryTest extends AllRepositoryTest {

  @Autowired
  private FolioInstanceAllRepository instanceAllRepository;

  @Test
  void findFolioInstanceAllNonDeletedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice = instanceAllRepository.findFolioInstanceAllNonDeleted(MIN_UUID, MAX_UUID, PageRequest.of(0, exportIdsBatch));
      assertThat(slice.getContent()).hasSize(2);
    }
  }

  @Test
  void findFolioInstanceAllNonDeletedNonSuppressedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice = instanceAllRepository.findFolioInstanceAllNonDeletedNonSuppressed(MIN_UUID, MAX_UUID, PageRequest.of(0, exportIdsBatch));
      assertThat(slice.getContent()).hasSize(2);
    }
  }

  @Test
  void findFolioInstanceAllDeletedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = instanceAllRepository.findFolioInstanceAllDeleted();
      assertThat(list).hasSize(3);
    }
  }

  @Test
  void findFolioInstanceAllDeletedNonSuppressedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = instanceAllRepository.findFolioInstanceAllDeletedNonSuppressed();
      assertThat(list).hasSize(2);
    }
  }
}
