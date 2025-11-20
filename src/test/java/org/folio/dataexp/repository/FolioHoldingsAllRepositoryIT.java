package org.folio.dataexp.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class FolioHoldingsAllRepositoryIT extends AllRepositoryBaseIT {

  @Autowired
  private FolioHoldingsAllRepository folioHoldingsAllRepository;

  @Test
  void findFolioHoldingsAllNonDeletedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice = folioHoldingsAllRepository.findFolioHoldingsAllNonDeleted(MIN_UUID, MAX_UUID,
          PageRequest.of(0, exportIdsBatch));
      assertThat(slice.getContent()).hasSize(3);
    }
  }

  @Test
  void findFolioHoldingsAllNonDeletedNonSuppressedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice = folioHoldingsAllRepository.findFolioHoldingsAllNonDeletedNonSuppressed(
          MIN_UUID, MAX_UUID, PageRequest.of(0, exportIdsBatch));
      assertThat(slice.getContent()).hasSize(2);
    }
  }

  @Test
  void findFolioHoldingsAllDeletedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = folioHoldingsAllRepository.findFolioHoldingsAllDeleted();
      assertThat(list).hasSize(3);
    }
  }

  @Test
  void findFolioHoldingsAllDeletedNonSuppressedTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = folioHoldingsAllRepository.findFolioHoldingsAllDeletedNonSuppressed();
      assertThat(list).hasSize(2);
    }
  }

  @Test
  void findFolioHoldingsAllNonDeletedCustomHoldingsProfileTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice = folioHoldingsAllRepository.findMarcHoldingsAllNonDeletedCustomHoldingsProfile(
          MIN_UUID, MAX_UUID, PageRequest.of(0, exportIdsBatch));
      assertThat(slice.getContent()).hasSize(3);
    }
  }

  @Test
  void findFolioHoldingsAllNonDeletedNonSuppressedCustomHoldingsProfileTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var slice = folioHoldingsAllRepository
          .findMarcHoldingsAllNonDeletedNonSuppressedCustomHoldingsProfile(MIN_UUID, MAX_UUID,
              PageRequest.of(0, exportIdsBatch));
      assertThat(slice.getContent()).hasSize(2);
    }
  }

  @Test
  void findFolioHoldingsAllDeletedCustomHoldingsProfileTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = folioHoldingsAllRepository.findMarcHoldingsAllDeletedCustomHoldingsProfile();
      assertThat(list).hasSize(3);
    }
  }

  @Test
  void findFolioHoldingsAllDeletedNonSuppressedCustomHoldingsProfileTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var list = folioHoldingsAllRepository
          .findMarcHoldingsAllDeletedNonSuppressedCustomHoldingsProfile();
      assertThat(list).hasSize(2);
    }
  }
}
