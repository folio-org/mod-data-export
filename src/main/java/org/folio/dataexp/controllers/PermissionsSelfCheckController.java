package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.service.UserPermissionsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/data-export")
@RequiredArgsConstructor
@Log4j2
public class PermissionsSelfCheckController implements org.folio.dataexp.rest.resource.PermissionsSelfCheckApi {

  private final UserPermissionsService userPermissionsService;

  @Override
  public ResponseEntity<List<String>> getUsersPermissions() {
    return new ResponseEntity<>(userPermissionsService.getPermissions(), HttpStatus.OK);
  }
}
