package org.folio.util;

import org.apache.http.HttpStatus;
import org.junit.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExceptionToResponseMapperUnitTest {


  @Test
  public void shouldReturnBadRequestResponse() {
    Response response = ExceptionToResponseMapper.map(new BadRequestException("Bad request message"));
    assertNotNull(response);
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertEquals("Bad request message", response.getEntity().toString());
  }

  @Test
  public void shouldReturnNotFoundResponse() {
    Response response = ExceptionToResponseMapper.map(new NotFoundException("Not found message"));
    assertNotNull(response);
    assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertEquals("Not found message", response.getEntity().toString());
  }

  @Test
  public void shouldReturnInternalServerErrorResponse() {
    Response response = ExceptionToResponseMapper.map(new InternalServerErrorException("Internal server error message"));
    assertNotNull(response);
    assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertTrue(response.getEntity().toString().contains("Internal Server Error"));
  }
}
