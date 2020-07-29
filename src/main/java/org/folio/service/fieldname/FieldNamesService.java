package org.folio.service.fieldname;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FieldName;
import org.folio.rest.jaxrs.model.FieldNameCollection;
import org.folio.util.OkapiConnectionParams;

public interface FieldNamesService {

  /**
   * Searches for {@link FieldNameCollection} by id
   *
   * @param okapiConnectionParams okapi headers and connection parameters
   * @return future with list of {@link FieldName}
   */
  Future<FieldNameCollection> getFieldNames(OkapiConnectionParams okapiConnectionParams);

}
