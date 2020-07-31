package org.folio.service.fieldname;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.TransformationField;
import org.folio.rest.jaxrs.model.TransformationFieldCollection;
import org.folio.util.OkapiConnectionParams;

public interface TransformationFieldsService {

  /**
   * An entry point for retrieving transformation fields {@link TransformationFieldCollection}
   *
   * @param okapiConnectionParams okapi headers and connection parameters
   * @return future with list of {@link TransformationField}
   */
  Future<TransformationFieldCollection> getTransformationFields(OkapiConnectionParams okapiConnectionParams);

}
