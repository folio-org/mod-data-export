package org.folio.dao.profiles;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.PropertyKind;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import java.util.List;
import org.folio.dao.impl.JobExecutionDaoImpl;
import org.folio.dao.impl.PostgresClientFactory;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.Criteria.Criterion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class JobExecutionDaoUnitTest {
  private static final String TABLE = "job_executions";
  private static final String TENANT_ID = "diku";
  private static final String ID = "54056375-e8e4-4c0e-a544-869aadfae716";
  private static JobExecution jobExecutions;
  @InjectMocks
  private JobExecutionDaoImpl jobExecutionDao;
  @Mock
  private PostgresClientFactory postgresClientFactory;
  @Mock
  private PostgresClient postgresClient;
  @Mock
  private AsyncResult<RowSet<Row>> updateResult;

  @BeforeAll
  public static void setUp() {
    jobExecutions = new JobExecution();
  }

  @Test
  void shouldFailToUpdateJobExecution_whenPgClientThrewException(VertxTestContext context) {
    // given
    when(postgresClientFactory.getInstance(TENANT_ID)).thenReturn(postgresClient);
    doThrow(RuntimeException.class)
      .when(postgresClient).update(eq(TABLE), eq(jobExecutions), any(Criterion.class), eq(true), any(Handler.class));

    // when
    Future<JobExecution> future = jobExecutionDao.update(jobExecutions, TENANT_ID);

    // then
    future.onComplete(ar -> {
      context.verify(() -> {
        assertTrue(ar.failed());
        verify(postgresClient).update(eq(TABLE), eq(jobExecutions), any(Criterion.class), eq(true), any(Handler.class));
        assertTrue(ar.cause() instanceof RuntimeException);
        context.completeNow();
      });
    });
  }

  @Test
  void shouldFailToUpdateJobExecution_whenPgClientTReturnedFailedFuture(VertxTestContext context) {
    // given
    when(updateResult.failed()).thenReturn(true);
    when(postgresClientFactory.getInstance(TENANT_ID)).thenReturn(postgresClient);
    doAnswer(invocationOnMock -> {
      Handler<AsyncResult<RowSet<Row>>> replyHandler = invocationOnMock.getArgument(4);
      replyHandler.handle(updateResult);
      return null;
    }).when(postgresClient).update(eq(TABLE), eq(jobExecutions), any(Criterion.class), eq(true), any(Handler.class));

    // when
    Future<JobExecution> future = jobExecutionDao.update(jobExecutions, TENANT_ID);

    // then
    future.onComplete(ar -> {
      context.verify(() -> {
        assertTrue(ar.failed());
        verify(postgresClient).update(eq(TABLE), eq(jobExecutions), any(Criterion.class), eq(true), any(Handler.class));
        assertTrue(ar.cause() instanceof RuntimeException);
        context.completeNow();
      });
    });
  }

  @Test
  void shouldFailToDeleteJobExecution_whenPgClientReturnedFailedFuture(VertxTestContext context) {
    // given
    when(postgresClientFactory.getInstance(TENANT_ID)).thenReturn(postgresClient);
    doAnswer(invocationOnMock -> {
      Handler<AsyncResult<RowSet<Row>>> replyHandler = invocationOnMock.getArgument(2);
      replyHandler.handle(Future.failedFuture("Connection Failure"));
      return null;
    }).when(postgresClient).delete(eq(TABLE), eq(ID), any(Handler.class));

    // when
    Future<Boolean> future = jobExecutionDao.deleteById(ID, TENANT_ID);

    // then
    future.onComplete(ar -> {
      context.verify(() -> {
        assertTrue(ar.failed());
        verify(postgresClient).delete(eq(TABLE), eq(ID), any(Handler.class));
        context.completeNow();
      });
    });
  }

  /**
   *
   * THis method can be removed once we have an API for deleting job execution
   * @param context
   */
  @Test
  void shouldSuceedDeleteJobExecution_whenPgClientReturnedSuceededFuture(VertxTestContext context) {
    // given
    RowSet<Row> promise = new RowSet<Row>() {

      @Override
      public int rowCount() {
        return 1;
      }

      @Override
      public List<String> columnsNames() {
        return null;
      }

      @Override
      public int size() {
        return 0;
      }

      @Override
      public <V> V property(PropertyKind<V> propertyKind) {
        return null;
      }

      @Override
      public RowSet<Row> value() {
        return null;
      }

      @Override
      public RowIterator<Row> iterator() {
        return null;
      }

      @Override
      public RowSet<Row> next() {
        return null;
      }
    };
    updateResult = new AsyncResult<RowSet<Row>>() {

      @Override
      public boolean succeeded() {
        return true;
      }

      @Override
      public RowSet<Row> result() {
        return promise;
      }

      @Override
      public boolean failed() {
        return false;
      }

      @Override
      public Throwable cause() {
        return null;
      }
    };
    when(postgresClientFactory.getInstance(TENANT_ID)).thenReturn(postgresClient);
    doAnswer(invocationOnMock -> {
      Handler<AsyncResult<RowSet<Row>>> replyHandler = invocationOnMock.getArgument(2);
      replyHandler.handle(updateResult);
      return null;
    }).when(postgresClient).delete(eq(TABLE), eq(ID), any(Handler.class));

    // when
    Future<Boolean> future = jobExecutionDao.deleteById(ID, TENANT_ID);

    // then
    future.onComplete(ar -> {
      context.verify(() -> {
        assertTrue(ar.result());
        verify(postgresClient).delete(eq(TABLE), eq(ID), any(Handler.class));
        context.completeNow();
      });
    });
  }
}
