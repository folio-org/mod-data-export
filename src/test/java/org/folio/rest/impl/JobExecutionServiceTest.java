package org.folio.rest.impl;

import io.restassured.RestAssured;
import io.vertx.core.Context;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobExecution.Status;
import org.folio.service.job.JobExecutionService;
import org.folio.spring.SpringContextUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class JobExecutionServiceTest extends RestVerticleTestBase {

  @Autowired
  JobExecutionService jobExecutionService;

  public JobExecutionServiceTest() {
    Context vertxContext = vertx.getOrCreateContext();
    SpringContextUtil.init(vertxContext.owner(), vertxContext, DataExportTest.TestMock.class);
    SpringContextUtil.autowireDependencies(this, vertxContext);
  }

  @Test
  void getJobExecutions_return200Status_forHappyPath() {
    RestAssured.given()
      .spec(jsonRequestSpecification)
      .when()
      .get(JOB_EXECUTIONS_URL + "/" + "?query=status=FAIL")
      .then()
      .statusCode(HttpStatus.SC_OK);
  }

  @Test
  void getJobExecutions_return200StatusWithEmptyList_IfNoJobExecutionsWithGivenId() {
    RestAssured.given()
      .spec(jsonRequestSpecification)
      .when()
      .get(JOB_EXECUTIONS_URL + "/" + "?query=id=" + UUID.randomUUID().toString())
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("jobExecutions", empty())
      .body("totalRecords", is(0));
  }

  @Test
  void expireJobExecutions_return204_andChangeJobStatusToFail(VertxTestContext context) throws ParseException {
    String dateString = "2020-09-15T11:02:00.847+0000";
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSZ");
    Date date = dateFormat.parse(dateString);
    JobExecution jobExecution = new JobExecution()
      .withJobProfileId("6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a")
      .withStatus(Status.IN_PROGRESS)
      .withLastUpdatedDate(date);

    jobExecutionService.save(jobExecution, TENANT_ID);

    vertx.setTimer(3000L, handler -> {

      RestAssured.given()
        .spec(jsonRequestSpecification)
        .when()
        .post(EXPIRE_JOBS_URL)
        .then()
        .statusCode(HttpStatus.SC_NO_CONTENT);

      vertx.setTimer(3000L, ar ->
        jobExecutionService.getById(jobExecution.getId(), TENANT_ID)
          .onSuccess(jobExec -> context.verify(() -> {
            Assertions.assertEquals(Status.FAIL, jobExec.getStatus());
            context.completeNow();
          })));
    });
  }

}
