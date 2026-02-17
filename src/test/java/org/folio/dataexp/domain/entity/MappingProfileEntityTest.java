package org.folio.dataexp.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.junit.jupiter.api.Test;

class MappingProfileEntityTest {

  @Test
  @TestMate(name = "TestMate-d786d6d04e9a68c1acfa18e0a0562872")
  void testFromMappingProfileShouldGenerateIdWhenIdIsNull() {
    // Given
    var mappingProfile = new MappingProfile();
    // When
    var resultEntity = MappingProfileEntity.fromMappingProfile(mappingProfile);
    // Then
    assertThat(mappingProfile.getId()).isNotNull();
    assertThat(resultEntity.getId()).isEqualTo(mappingProfile.getId());
    assertThat(resultEntity.getMappingProfile()).isSameAs(mappingProfile);
  }
}
