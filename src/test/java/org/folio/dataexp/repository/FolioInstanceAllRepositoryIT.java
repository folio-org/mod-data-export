package org.folio.dataexp.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class FolioInstanceAllRepositoryIT extends AllRepositoryBaseIT {

  @Autowired private FolioInstanceAllRepository instanceAllRepository;

  @Test
  void findFolioInstanceAllNonDeletedTest() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice =
          instanceAllRepository.findFolioInstanceAllNonDeleted(
              MIN_UUID, MAX_UUID, PageRequest.of(0, EXPORT_IDS_BATCH));
      assertThat(slice.getContent()).hasSize(2);
    }
  }

  @Test
  void findFolioInstanceAllNonDeletedNonSuppressedTest() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice =
          instanceAllRepository.findFolioInstanceAllNonDeletedNonSuppressed(
              MIN_UUID, MAX_UUID, PageRequest.of(0, EXPORT_IDS_BATCH));
      assertThat(slice.getContent()).hasSize(2);
    }
  }

  @Test
  void findFolioInstanceAllDeletedNonSuppressedTest() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = instanceAllRepository.findFolioInstanceAllDeletedNonSuppressed();
      assertThat(list).hasSize(2);
    }
  }

  @Test
  void findMarcInstanceAllNonDeletedCustomInstanceProfileTest() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice =
          instanceAllRepository.findMarcInstanceAllNonDeletedCustomInstanceProfile(
              MIN_UUID, MAX_UUID, PageRequest.of(0, EXPORT_IDS_BATCH));
      assertThat(slice).hasSize(2);
    }
  }

  @Test
  void findMarcInstanceAllNonDeletedNonSuppressedForCustomInstanceProfileTest() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice =
          instanceAllRepository.findMarcInstanceAllNonDeletedNonSuppressedForCustomInstanceProfile(
              MIN_UUID, MAX_UUID, PageRequest.of(0, EXPORT_IDS_BATCH));
      assertThat(slice).hasSize(1);
    }
  }

  @Test
  void findMarcInstanceAllDeletedForCustomInstanceProfileTest() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = instanceAllRepository.findMarcInstanceAllDeletedForCustomInstanceProfile();
      assertThat(list).hasSize(6);
    }
  }

  @Test
  void findMarcInstanceAllDeletedNonSuppressedCustomInstanceProfileTest() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var list =
          instanceAllRepository.findMarcInstanceAllDeletedNonSuppressedCustomInstanceProfile();
      assertThat(list).hasSize(3);
    }
  }
}
