package org.folio.dataexp.controllers;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializerIT;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.repository.JobExecutionEntityCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class JobExecutionsControllerIT extends BaseDataExportInitializerIT {

  @MockitoBean
  private JobExecutionEntityCqlRepository jobExecutionEntityCqlRepository;

  @Test
  @SneakyThrows
  void getJobExecutionsTest() {
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());


    var entity = JobExecutionEntity.builder().id(jobExecution.getId()).jobExecution(jobExecution)
        .build();
    PageImpl<JobExecutionEntity> page = new PageImpl<>(List.of(entity));

    when(jobExecutionEntityCqlRepository.findByCql(isA(String.class), isA(OffsetRequest.class)))
        .thenReturn(page);

    mockMvc.perform(MockMvcRequestBuilders
        .get("/data-export/job-executions?query=query")
        .headers(defaultHeaders()))
        .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  void deleteJobExecutionByIdTest() {
    mockMvc.perform(MockMvcRequestBuilders
        .delete("/data-export/job-executions/" + UUID.randomUUID())
        .headers(defaultHeaders()))
        .andExpect(status().isNoContent());
  }
}
