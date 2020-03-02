package org.folio.rest.impl;

import io.restassured.RestAssured;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.folio.rest.RestVerticleTestBase;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

@RunWith(VertxUnitRunner.class)
public class JobExecutionServiceTest extends RestVerticleTestBase {

  private static final String JOB_EXECUTIONS_URL = "/data-export/jobExecutions";

  @Test
  public void getJobExecutions_return200Status_forHappyPath(TestContext context) {
    RestAssured.given()
      .spec(jsonRequestSpecification)
      .when()
      .get(JOB_EXECUTIONS_URL + "/" + "?query=status=FAIL")
      .then()
      .statusCode(HttpStatus.SC_OK);
  }

  @Test
  public void getJobExecutions_return200StatusWithEmptyList_IfNoJobExecutionsWithGivenId(TestContext context) {
    RestAssured.given()
      .spec(jsonRequestSpecification)
      .when()
      .get(JOB_EXECUTIONS_URL + "/" + "?query=id=" + UUID.randomUUID().toString())
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("jobExecutions", empty())
      .body("totalRecords", is(0));
  }

}
