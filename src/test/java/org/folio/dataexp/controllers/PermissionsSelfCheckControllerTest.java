package org.folio.dataexp.controllers;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.client.OkapiUserPermissionsClient;
import org.folio.dataexp.domain.dto.UserPermissions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PermissionsSelfCheckControllerTest extends BaseDataExportInitializer {

  @MockBean
  private OkapiUserPermissionsClient okapiUserPermissionsClient;

  @Test
  @SneakyThrows
  void postCleanUpFiles() {
    when(okapiUserPermissionsClient.getPermissions(any(String.class))).thenReturn(
      new UserPermissions().withPermissionNames(List.of("some permission")));
    mockMvc.perform(MockMvcRequestBuilders
        .get("/data-export/permissions-self-check")
        .headers(defaultHeaders()))
      .andExpect(status().isOk());
  }
}
