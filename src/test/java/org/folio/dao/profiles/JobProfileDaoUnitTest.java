package org.folio.dao.profiles;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.impl.JobProfileDaoImpl;
import org.folio.dao.impl.PostgresClientFactory;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class JobProfileDaoUnitTest {
  private static final String TABLE = "job_profiles";
  private static final String TENANT_ID = "diku";
  private static JobProfile jobProfile;
  @InjectMocks
  private JobProfileDaoImpl jobProfileDao;
  @Mock
  private PostgresClientFactory postgresClientFactory;
  @Mock
  private PostgresClient postgresClient;
  @Mock
  private AsyncResult<RowSet<Row>> updateResult;

  @BeforeAll
  public static void setUp() {
    jobProfile = new JobProfile()
      .withId(UUID.randomUUID().toString());
  }

  @Test
  void shouldFailToUpdateJobExecution_whenPgClientThrewException(VertxTestContext context) {
    // given
    when(postgresClientFactory.getInstance(TENANT_ID)).thenReturn(postgresClient);
    doThrow(RuntimeException.class)
      .when(postgresClient).update(eq(TABLE), eq(jobProfile), any(Criterion.class), eq(true), any(Handler.class));

    // when
    Future<JobProfile> future = jobProfileDao.update(jobProfile, TENANT_ID);

    // then
    future.onComplete(ar -> {
      context.verify(() -> {
        assertTrue(ar.failed());
        verify(postgresClient).update(eq(TABLE), eq(jobProfile), anyString(), any(Handler.class));
        assertTrue(ar.cause() instanceof RuntimeException);
        context.completeNow();
      });
    });
  }

  @Test
  void shouldFailToUpdateJobExecution_whenPgClientReturnedFailedFuture(VertxTestContext context) {
    // given
    when(updateResult.failed()).thenReturn(true);
    when(postgresClientFactory.getInstance(TENANT_ID)).thenReturn(postgresClient);
    doAnswer(invocationOnMock -> {
      Handler<AsyncResult<RowSet<Row>>> replyHandler = invocationOnMock.getArgument(3);
      replyHandler.handle(updateResult);
      return null;
    }).when(postgresClient).update(eq(TABLE), eq(jobProfile), anyString(), any(Handler.class));

    // when
    Future<JobProfile> future = jobProfileDao.update(jobProfile, TENANT_ID);

    // then
    future.onComplete(ar -> {
      context.verify(() -> {
        assertTrue(ar.failed());
        verify(postgresClient).update(eq(TABLE), eq(jobProfile), anyString(), any(Handler.class));
        assertTrue(ar.cause() instanceof RuntimeException);
        context.completeNow();
      });
    });
  }

}
