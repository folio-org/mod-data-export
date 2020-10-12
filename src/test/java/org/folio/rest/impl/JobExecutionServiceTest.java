package org.folio.rest.impl;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

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

import io.restassured.RestAssured;
import io.vertx.core.Context;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

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

      jobExecutionService.getById(jobExecution.getId(), TENANT_ID)
        .onComplete(asyncResult -> context.verify(() -> {
          Assertions.assertTrue(asyncResult.succeeded());
          Assertions.assertEquals(Status.FAIL, asyncResult.result().getStatus());
          context.completeNow();
        }));
    });
  }

  @Test
  void deleteJobExecutions_return404_IfNoJobExecutionsWithGivenId() {
    RestAssured.given()
      .spec(jsonRequestSpecification)
      .when()
      .delete(JOB_EXECUTIONS_URL + "/" + UUID.randomUUID().toString())
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND);
  }

  @Test
  void deleteJobExecutions_return200_IfJobExecutionWithGivenIdPresent(VertxTestContext context) {
    //create a Job Execution
    JobExecution jobExecution = new JobExecution().withId(UUID.randomUUID().toString()).withStatus(Status.IN_PROGRESS);

    jobExecutionService.save(jobExecution, TENANT_ID);

    vertx.setTimer(3000L, handler -> {

      //delete of the above job execution is a success
      RestAssured.given().spec(jsonRequestSpecification).when()
        .delete(JOB_EXECUTIONS_URL + "/" + jobExecution.getId()).then()
        .statusCode(HttpStatus.SC_NO_CONTENT);

      //verify the jobexecution is not present
      jobExecutionService.getById(jobExecution.getId(), TENANT_ID)
        .onComplete(jobExec -> context.verify(() -> {
          Assertions.assertEquals(true, jobExec.failed());
          context.completeNow();
        }));
    });

  }

}
