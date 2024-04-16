package org.folio.dataexp.service.validators;

import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.exception.mapping.profile.MappingProfileFieldsSuppressionException;
import org.folio.dataexp.exception.mapping.profile.MappingProfileFieldsSuppressionPatternException;
import org.folio.dataexp.exception.mapping.profile.MappingProfileTransformationPatternException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MappingProfileValidatorTest {

  @Test
  void validateMappingProfileTransformationsTest() {
    var transformation = new Transformations();
    transformation.setFieldId("holdings.callnumber");
    transformation.setPath("$.holdings[*].callNumber");
    transformation.setRecordType(RecordTypes.HOLDINGS);
    transformation.setTransformation("90");
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");
    mappingProfile.setTransformations(List.of(transformation));

    var validator = new MappingProfileValidator();
    assertThrows(MappingProfileTransformationPatternException.class, () -> validator.validate(mappingProfile));
  }

  @Test
  void validateMappingProfileSuppressionTest() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");
    mappingProfile.setTransformations(List.of());
    var validator = new MappingProfileValidator();

    mappingProfile.setFieldsSuppression("90");
    assertThrows(MappingProfileFieldsSuppressionPatternException.class, () -> validator.validate(mappingProfile));

    mappingProfile.setFieldsSuppression("9000");
    assertThrows(MappingProfileFieldsSuppressionPatternException.class, () -> validator.validate(mappingProfile));

    mappingProfile.setFieldsSuppression("aab, 900");
    assertThrows(MappingProfileFieldsSuppressionPatternException.class, () -> validator.validate(mappingProfile));
  }

  @Test
  void validateMappingProfileSuppressionForItemRecordTest() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");
    mappingProfile.setTransformations(List.of());
    var validator = new MappingProfileValidator();
    mappingProfile.recordTypes(List.of(RecordTypes.ITEM));

    mappingProfile.setFieldsSuppression("900");
    assertThrows(MappingProfileFieldsSuppressionException.class, () -> validator.validate(mappingProfile));
  }
}
