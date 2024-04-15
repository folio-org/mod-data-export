package org.folio.dataexp.service.validators;

import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.exception.mapping.profile.MappingProfileSuppressionFieldPatternException;
import org.folio.dataexp.exception.mapping.profile.MappingProfileTransformationPatternException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class MappingProfileValidatorTest {

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

    mappingProfile.setSuppression(List.of("90"));
    assertThrows(MappingProfileSuppressionFieldPatternException.class, () -> validator.validate(mappingProfile));

    mappingProfile.setSuppression(List.of("9000"));
    assertThrows(MappingProfileSuppressionFieldPatternException.class, () -> validator.validate(mappingProfile));
  }
}
