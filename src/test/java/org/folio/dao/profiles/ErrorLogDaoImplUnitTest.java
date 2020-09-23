package org.folio.dao.profiles;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.folio.dao.impl.ErrorLogDaoImpl;
import org.folio.dao.impl.PostgresClientFactory;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class ErrorLogDaoImplUnitTest {

  private static final String TABLE = "error_logs";
  private static final String TENANT_ID = "diku";
  @InjectMocks
  private ErrorLogDaoImpl errorLogDao;
  @Mock
  private PostgresClientFactory postgresClientFactory;
  @Mock
  private PostgresClient postgresClient;
  private ErrorLog errorLog;

  @BeforeEach
  public void setUp() {
    errorLog = new ErrorLog()
      .withId(UUID.randomUUID().toString());
  }

  @Test
  void shouldFailToGetLogErrorCollection_whenPgClientThrewException(VertxTestContext context) {
    // given
    when(postgresClientFactory.getInstance(TENANT_ID)).thenReturn(postgresClient);
    doThrow(RuntimeException.class)
      .when(postgresClient).get(eq(TABLE), eq(ErrorLog.class), any(String[].class), any(CQLWrapper.class), any(Boolean.class), any(Boolean.class), any(Handler.class));

    // when
    Future<ErrorLogCollection> future = errorLogDao.getByJobExecutionId("jobExecutionid", 0, 0, TENANT_ID);

    // then
    future.onComplete(ar -> {
      context.verify(() -> {
        assertTrue(ar.failed());
        assertTrue(ar.cause() instanceof RuntimeException);
        context.completeNow();
      });
    });
  }

  @Test
  void shouldFailToUpdate_whenPgClientThrewException(VertxTestContext context) {
    // given
    when(postgresClientFactory.getInstance(TENANT_ID)).thenReturn(postgresClient);
    doThrow(RuntimeException.class)
      .when(postgresClient).update(eq(TABLE), eq(ErrorLog.class), any(Criterion.class), any(Boolean.class), any(Handler.class));

    // when
    Future<ErrorLog> future = errorLogDao.update(errorLog, TENANT_ID);

    // then
    future.onComplete(ar -> {
      context.verify(() -> {
        assertTrue(ar.failed());
        assertTrue(ar.cause() instanceof RuntimeException);
        context.completeNow();
      });
    });
  }

  @Test
  void shouldFailToDelete_whenPgClientThrewException(VertxTestContext context) {
    // given
    when(postgresClientFactory.getInstance(TENANT_ID)).thenReturn(postgresClient);
    doThrow(RuntimeException.class)
      .when(postgresClient).delete(eq(TABLE), any(Criterion.class), any(Handler.class));

    // when
    Future<Boolean> future = errorLogDao.deleteById(errorLog.getId(), TENANT_ID);

    // then
    future.onComplete(ar -> {
      context.verify(() -> {
        assertTrue(ar.failed());
        assertTrue(ar.cause() instanceof RuntimeException);
        context.completeNow();
      });
    });
  }

  @Test
  void shouldFailToGetById_whenPgClientThrewException(VertxTestContext context) {
    // given
    when(postgresClientFactory.getInstance(TENANT_ID)).thenReturn(postgresClient);
    doThrow(RuntimeException.class)
      .when(postgresClient).get(eq(TABLE), eq(ErrorLog.class), any(Criterion.class), any(Boolean.class), any(Handler.class));

    // when
    Future<Optional<ErrorLog>> future = errorLogDao.getById(errorLog.getId(), TENANT_ID);

    // then
    future.onComplete(ar -> {
      context.verify(() -> {
        assertTrue(ar.failed());
        assertTrue(ar.cause() instanceof RuntimeException);
        context.completeNow();
      });
    });
  }



}
