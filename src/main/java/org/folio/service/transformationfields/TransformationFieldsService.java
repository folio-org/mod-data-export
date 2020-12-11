package org.folio.service.transformationfields;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.TransformationField;
import org.folio.rest.jaxrs.model.TransformationFieldCollection;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.util.OkapiConnectionParams;

import java.util.List;

public interface TransformationFieldsService {

  /**
   * An entry point for retrieving transformation fields {@link TransformationFieldCollection}
   *
   * @param okapiConnectionParams okapi headers and connection parameters
   * @return future with list of {@link TransformationField}
   */
  Future<TransformationFieldCollection> getTransformationFields(OkapiConnectionParams okapiConnectionParams);

  /**
   * Validates transformations fields on the correctness of inputs for each separate
   * part of the transformation: tag, indicators and subfield.
   *
   * @param transformations - transformations to be validated
   * @return future
   */
  Future<Void> validateTransformations(List<Transformations> transformations);


}
