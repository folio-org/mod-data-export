package org.folio.dataexp.controllers;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializerIT;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionRunBy;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.repository.JobExecutionEntityCqlRepository;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class RelatedUsersControllerIT extends BaseDataExportInitializerIT {

  @Autowired
  private JobExecutionEntityCqlRepository jobExecutionEntityCqlRepository;

  @Test
  @SneakyThrows
  void getRelatedUsersTest() {

    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {

      var userId = UUID.randomUUID();
      var userId2 = UUID.randomUUID();

      var entities = List.of(
        new JobExecutionEntity().withJobExecution(new JobExecution().runBy(
          new JobExecutionRunBy().userId(userId.toString()).firstName("first name")
              .lastName("last name"))).withId(UUID.randomUUID()),
        new JobExecutionEntity().withJobExecution(new JobExecution().runBy(
            new JobExecutionRunBy().userId(userId2.toString()).firstName("first name 2")
                .lastName("last name 2"))).withId(UUID.randomUUID()),
        new JobExecutionEntity().withJobExecution(new JobExecution().runBy(
            new JobExecutionRunBy().userId(userId.toString()).firstName("first name")
              .lastName("last name"))).withId(UUID.randomUUID()));

      jobExecutionEntityCqlRepository.saveAll(entities);

      mockMvc.perform(MockMvcRequestBuilders
          .get("/data-export/related-users")
          .headers(defaultHeaders())
          .contentType(APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().json("{\n"
              + "    \"relatedUsers\": [{\n"
              + "            \"firstName\": \"first name\",\n"
              + "            \"lastName\": \"last name\",\n"
              + "            \"userId\": \"" + userId + "\"\n"
              + "        },"
              + "{\n"
              + "            \"firstName\": \"first name 2\",\n"
              + "            \"lastName\": \"last name 2\",\n"
              + "            \"userId\": \"" + userId2 + "\"\n"
              + "        }],\n"
              + "    \"totalRecords\": 2\n"
              + "}"));
    }
  }
}
