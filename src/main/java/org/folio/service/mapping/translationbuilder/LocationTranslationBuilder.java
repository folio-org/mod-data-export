package org.folio.service.mapping.translationbuilder;

import com.google.common.base.Splitter;
import org.folio.processor.translations.Translation;
import org.folio.rest.jaxrs.model.Transformations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.folio.util.ExternalPathResolver.CAMPUSES;
import static org.folio.util.ExternalPathResolver.INSTITUTIONS;
import static org.folio.util.ExternalPathResolver.LIBRARIES;

public class LocationTranslationBuilder implements TranslationBuilder {

  private static final String FIELD_PARAM_KEY = "field";
  private static final String REFERENCE_DATA_PARAM_KEY = "referenceData";
  private static final String REFERENCE_DATA_ID_FIELD = "referenceDataIdField";
  private static final String LIBRARY_KEY = "library";
  private static final String LIBRARY_ID_FIELD = "libraryId";
  private static final String CAMPUS_ID_FIELD = "campusId";
  private static final String INSTITUTION_ID_FIELD = "institutionId";
  private static final String CAMPUS_KEY = "campus";
  private static final String INSTITUTION_KEY = "institution";

  @Override
  public Translation build(String functionName, Transformations mappingTransformation) {
    Translation translation = new Translation();
    translation.setFunction(functionName);
    List<String> locationParts = Splitter.on(".").splitToList(mappingTransformation.getFieldId());
    Map<String, String> parameters = new HashMap<>();
    if (locationParts.size() == 3) {
      parameters.put(FIELD_PARAM_KEY, locationParts.get(2));
    } else if (locationParts.size() == 4) {
      String referenceDataType = locationParts.get(2);
      String field = locationParts.get(3);
      parameters.put(FIELD_PARAM_KEY, field);
      if (LIBRARY_KEY.equals(referenceDataType)) {
        parameters.put(REFERENCE_DATA_PARAM_KEY, LIBRARIES);
        parameters.put(REFERENCE_DATA_ID_FIELD, LIBRARY_ID_FIELD);
      } else if (CAMPUS_KEY.equals(referenceDataType)) {
        parameters.put(REFERENCE_DATA_PARAM_KEY, CAMPUSES);
        parameters.put(REFERENCE_DATA_ID_FIELD, CAMPUS_ID_FIELD);
      } else if (INSTITUTION_KEY.equals(referenceDataType)) {
        parameters.put(REFERENCE_DATA_PARAM_KEY, INSTITUTIONS);
        parameters.put(REFERENCE_DATA_ID_FIELD, INSTITUTION_ID_FIELD);
      }
    }
    translation.setParameters(parameters);
    return translation;
  }
}
