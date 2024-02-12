package org.folio.dataexp.repository;

import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class MarcInstanceAllRepositoryTest extends AllRepositoryTest {

  @Autowired
  private MarcInstanceAllRepository marcInstanceAllRepository;

  @Test
  void findMarcInstanceAllNonDeletedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice = marcInstanceAllRepository.findMarcInstanceAllNonDeleted(MIN_UUID, MAX_UUID, PageRequest.of(0, exportIdsBatch));
      assertThat(slice.getContent().size()).isEqualTo(2);
    }
  }

  @Test
  void findMarcInstanceAllNonDeletedNonSuppressedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice = marcInstanceAllRepository.findMarcInstanceAllNonDeletedNonSuppressed(MIN_UUID, MAX_UUID, PageRequest.of(0, exportIdsBatch));
      assertThat(slice.getContent().size()).isEqualTo(1);
    }
  }

  @Test
  void findMarcInstanceAllDeletedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = marcInstanceAllRepository.findMarcInstanceAllDeleted();
      assertThat(list.size()).isEqualTo(15);
    }
  }

  @Test
  void findMarcInstanceAllDeletedNonSuppressedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = marcInstanceAllRepository.findMarcInstanceAllDeletedNonSuppressed();
      assertThat(list.size()).isEqualTo(8);
    }
  }
}
