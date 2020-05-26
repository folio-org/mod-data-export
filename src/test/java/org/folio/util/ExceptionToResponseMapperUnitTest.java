package org.folio.util;

import org.apache.http.HttpStatus;
import org.folio.rest.exceptions.ServiceException;
import org.junit.jupiter.api.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

class ExceptionToResponseMapperUnitTest {


  @Test
  void shouldReturnBadRequestResponse() {
    Response response = ExceptionToResponseMapper.map(new BadRequestException("Bad request message"));
    assertNotNull(response);
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertEquals("Bad request message", response.getEntity().toString());
  }

  @Test
  void shouldReturnNotFoundResponse() {
    Response response = ExceptionToResponseMapper.map(new NotFoundException("Not found message"));
    assertNotNull(response);
    assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertEquals("Not found message", response.getEntity().toString());
  }

  @Test
  void shouldReturnInternalServerErrorResponse() {
    Response response = ExceptionToResponseMapper.map(new InternalServerErrorException("Internal server error message"));
    assertNotNull(response);
    assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertTrue(response.getEntity().toString().contains("Internal Server Error"));
  }


  @Test
  void shouldReturnHTTPResponse() {
    Response response = ExceptionToResponseMapper.map(new ServiceException(org.folio.HttpStatus.HTTP_BAD_REQUEST, "Testing Http Exception"));
    assertNotNull(response);
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, response.getMediaType().toString());
    assertTrue(response.getEntity().toString().contains("Testing Http Exception"));
  }
}
