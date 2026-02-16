package org.folio.dataexp.domain.entity;

import org.junit.jupiter.api.Test;
import org.folio.dataexp.domain.dto.MappingProfile;
import static org.assertj.core.api.Assertions.assertThat;

class MappingProfileEntityTest {

  @Test
  void fromMappingProfile() {}

    @Test
  void testFromMappingProfileShouldGenerateIdWhenIdIsNull() {
    // TestMate-d786d6d04e9a68c1acfa18e0a0562872
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
