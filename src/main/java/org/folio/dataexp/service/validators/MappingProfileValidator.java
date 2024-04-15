package org.folio.dataexp.service.validators;

import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.Errors;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.ParametersInner;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.exception.mapping.profile.MappingProfileSuppressionFieldPatternException;
import org.folio.dataexp.exception.mapping.profile.MappingProfileTransformationEmptyException;
import org.folio.dataexp.exception.mapping.profile.MappingProfileTransformationPatternException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class MappingProfileValidator {

  private static final String VALIDATION_ERROR_MESSAGE = "Mapping profile validation exception";
  private static final String ERROR_CODE = "javax.validation.constraints.Pattern.message";
  private static final String ERROR_VALIDATION_MESSAGE_PATTERN = "must match \\\"%s\\\"";

  private static final Pattern TRANSFORMATION_PATTERN = Pattern.compile("((\\d{3}([\\s]|[\\d]|[a-zA-Z]){2}(\\$([a-zA-Z]|[\\d]{1,2}))?)|(^$))");
  private static final String ERROR_VALIDATION_TRANSFORMATIONS_PARAMETER_KEY_PATTERN = "transformations[%s].transformation";
  private static final String TRANSFORMATION_ITEM_EMPTY_VALUE_MESSAGE = "Transformations for fields with item record type cannot be empty. Please provide a value.";

  private static final Pattern SUPPRESSION_FIELD_PATTERN = Pattern.compile("^\\d{3}$");
  private static final String ERROR_VALIDATION_SUPPRESSION_FIELD_PARAMETER_KEY_PATTERN = "suppressionFields[%s]";


  public void validate(MappingProfile mappingProfile) {
    validateMappingProfileTransformations(mappingProfile);
    validateMappingProfileSuppression(mappingProfile);
  }


  private void validateMappingProfileTransformations(MappingProfile mappingProfile) {
    var transformations = mappingProfile.getTransformations();
    var parameters = new ArrayList<ParametersInner>();
    for (int i = 0; i < transformations.size(); i++) {
      var transformation = transformations.get(i);
      var matcher = TRANSFORMATION_PATTERN.matcher(transformation.getTransformation());
      if (!matcher.matches()) {
        var parameter = ParametersInner.builder()
          .key(String.format(ERROR_VALIDATION_TRANSFORMATIONS_PARAMETER_KEY_PATTERN, i))
          .value(transformation.getTransformation()).build();
        parameters.add(parameter);
      }
    }
    if (!parameters.isEmpty()) {
      var errors = new Errors();
      for (var parameter : parameters) {
        var errorItem = new org.folio.dataexp.domain.dto.Error();
        errorItem.setCode(ERROR_CODE);
        errorItem.type("1");
        errorItem.message(String.format(ERROR_VALIDATION_MESSAGE_PATTERN, TRANSFORMATION_PATTERN));
        errors.addErrorsItem(errorItem);
        errorItem.setParameters(List.of(parameter));
      }
      errors.setTotalRecords(errors.getErrors().size());
      throw new MappingProfileTransformationPatternException(VALIDATION_ERROR_MESSAGE, errors);
    }
    for (var transformation : transformations) {
      if (StringUtils.isEmpty(transformation.getTransformation()) && transformation.getRecordType() == RecordTypes.ITEM) {
        throw new MappingProfileTransformationEmptyException(TRANSFORMATION_ITEM_EMPTY_VALUE_MESSAGE);
      }
    }
  }

  private void validateMappingProfileSuppression(MappingProfile mappingProfile) {
    var suppressionFields = mappingProfile.getSuppression();
    if (Objects.nonNull(suppressionFields)) {
      var parameters = new ArrayList<ParametersInner>();
      for (int i = 0; i < suppressionFields.size(); i++) {
        var suppression = suppressionFields.get(i);
        var matcher = SUPPRESSION_FIELD_PATTERN.matcher(suppression);
        if (!matcher.matches()) {
          var parameter = ParametersInner.builder()
            .key(String.format(ERROR_VALIDATION_SUPPRESSION_FIELD_PARAMETER_KEY_PATTERN, i))
            .value(suppression).build();
          parameters.add(parameter);
        }
      }
      if (!parameters.isEmpty()) {
        var errors = new Errors();
        for (var parameter : parameters) {
          var errorItem = new org.folio.dataexp.domain.dto.Error();
          errorItem.setCode(ERROR_CODE);
          errorItem.type("1");
          errorItem.message(String.format(ERROR_VALIDATION_MESSAGE_PATTERN, SUPPRESSION_FIELD_PATTERN));
          errors.addErrorsItem(errorItem);
          errorItem.setParameters(List.of(parameter));
        }
        errors.setTotalRecords(errors.getErrors().size());
        throw new MappingProfileSuppressionFieldPatternException(VALIDATION_ERROR_MESSAGE, errors);
      }
    }
  }
}
