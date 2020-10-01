package org.folio.service.logs;

import io.vertx.core.Future;
import org.folio.dao.ErrorLogDao;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ErrorLogServiceImpl implements ErrorLogService {

  @Autowired
  private ErrorLogDao errorLogDao;

  @Override
  public Future<ErrorLogCollection> getByJobExecutionId(String query, int offset, int limit, String tenantId) {
    return errorLogDao.getByJobExecutionId(query, offset, limit, tenantId);
  }

  @Override
  public Future<ErrorLog> save(ErrorLog errorLog, OkapiConnectionParams params) {
    if (errorLog.getId() == null) {
      errorLog.setId(UUID.randomUUID().toString());
    }
    return errorLogDao.save(errorLog, params.getTenantId());
  }

  @Override
  public Future<ErrorLog> update(ErrorLog errorLog, OkapiConnectionParams params) {
    return errorLogDao.update(errorLog, params.getTenantId());
  }

  @Override
  public Future<Boolean> deleteById(String id, String tenantId) {
    return errorLogDao.deleteById(id, tenantId);
  }
}
