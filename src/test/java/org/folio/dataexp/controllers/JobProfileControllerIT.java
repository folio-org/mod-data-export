package org.folio.dataexp.controllers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializerIT;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.User;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.repository.JobProfileEntityCqlRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.spring.data.OffsetRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class JobProfileControllerIT extends BaseDataExportInitializerIT {

  @MockitoBean
  private JobProfileEntityRepository jobProfileEntityRepository;
  @MockitoBean
  private JobProfileEntityCqlRepository jobProfileEntityCqlRepository;
  @MockitoBean
  private UserClient userClient;

  @Test
  @SneakyThrows
  void deleteJobProfileByIdTest() {
    var jobProfile = new JobProfile();
    jobProfile.setId(UUID.randomUUID());
    jobProfile.setDefault(false);
    jobProfile.setName("jobProfile");

    var entity = JobProfileEntity.builder().id(jobProfile.getId()).jobProfile(jobProfile).build();

    when(jobProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);

    mockMvc.perform(MockMvcRequestBuilders
        .delete("/data-export/job-profiles/" + jobProfile.getId().toString())
        .headers(defaultHeaders()))
        .andExpect(status().isNoContent());

    verify(jobProfileEntityRepository).deleteById(isA(UUID.class));
  }

  @Test
  @SneakyThrows
  void deleteDefaultJobProfileByIdTest() {
    var jobProfile = new JobProfile();
    jobProfile.setId(UUID.randomUUID());
    jobProfile.setDefault(true);
    jobProfile.setName("jobProfile");

    var entity = JobProfileEntity.builder().id(jobProfile.getId()).jobProfile(jobProfile).build();

    when(jobProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);

    mockMvc.perform(MockMvcRequestBuilders
        .delete("/data-export/job-profiles/" + jobProfile.getId().toString())
        .headers(defaultHeaders()))
        .andExpect(status().isForbidden());

    verify(jobProfileEntityRepository, times(0)).deleteById(isA(UUID.class));
  }

  @Test
  @SneakyThrows
  void getJobProfileByIdTest() {
    var jobProfile = new JobProfile();
    jobProfile.setId(UUID.randomUUID());
    jobProfile.setDefault(true);
    jobProfile.setName("jobProfile");

    var entity = JobProfileEntity.builder().id(jobProfile.getId()).jobProfile(jobProfile).build();

    when(jobProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);

    mockMvc.perform(MockMvcRequestBuilders
        .get("/data-export/job-profiles/" + jobProfile.getId().toString())
        .headers(defaultHeaders()))
        .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  void getJobProfilesTest() {
    var jobProfile = new JobProfile();
    jobProfile.setId(UUID.randomUUID());
    jobProfile.setDefault(true);
    jobProfile.setName("jobProfile");

    var entity = JobProfileEntity.builder().id(jobProfile.getId()).jobProfile(jobProfile).build();
    PageImpl<JobProfileEntity> page = new PageImpl<>(List.of(entity));

    when(jobProfileEntityCqlRepository.findByCql(isA(String.class), isA(OffsetRequest.class)))
        .thenReturn(page);

    mockMvc.perform(MockMvcRequestBuilders
        .get("/data-export/job-profiles?query=query")
        .headers(defaultHeaders()))
        .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  void getListOfJobProfilesUsedInCompletedJobsTest() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();

    Object[] profileData1 = new Object[2];
    profileData1[0] = id1;
    profileData1[1] = "Default instances export job profile 1";

    Object[] profileData2 = new Object[2];
    profileData2[0] = id2;
    profileData2[1] = "Default instances export job profile 2";

    List<Object[]> jobProfileData = Arrays.asList(profileData1, profileData2);

    when(jobProfileEntityCqlRepository.getUsedJobProfilesData(isA(Integer.class),
        isA(Integer.class))).thenReturn(jobProfileData);

    mockMvc.perform(MockMvcRequestBuilders
        .get("/data-export/job-profiles?used=true")
        .headers(defaultHeaders()))
        .andExpect(status().isOk())
        .andExpect(result -> {
          var usedJobProfilesResponse = new JSONObject(result.getResponse().getContentAsString());
          assertThat(usedJobProfilesResponse.getInt("totalRecords") == 2);
          assertThat(usedJobProfilesResponse.getJSONArray("jobProfiles").length() == 2);
        });
  }

  @Test
  @SneakyThrows
  void postJobProfileTest() {
    var jobProfile = new JobProfile();
    jobProfile.setId(UUID.randomUUID());
    jobProfile.setDefault(true);
    jobProfile.setName("jobProfile");
    jobProfile.setMappingProfileId(UUID.randomUUID());
    var user = new User();
    user.setPersonal(new User.Personal());

    var entity = JobProfileEntity.builder().id(jobProfile.getId()).jobProfile(jobProfile).build();
    when(jobProfileEntityRepository.save(isA(JobProfileEntity.class))).thenReturn(entity);
    when(userClient.getUserById(isA(String.class))).thenReturn(user);

    mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/job-profiles")
        .headers(defaultHeaders())
        .content(asJsonString(jobProfile)))
        .andExpect(status().isCreated());

    verify(jobProfileEntityRepository).save(isA(JobProfileEntity.class));
  }

  @Test
  @SneakyThrows
  void putJobProfileTest() {
    var jobProfile = new JobProfile();
    jobProfile.setId(UUID.randomUUID());
    jobProfile.setDefault(false);
    jobProfile.setName("jobProfile");
    jobProfile.setMappingProfileId(UUID.randomUUID());
    jobProfile.setMetadata(new Metadata().createdDate(new Date()));
    var user = new User();
    user.setPersonal(new User.Personal());

    var entity = JobProfileEntity.builder().id(jobProfile.getId()).jobProfile(jobProfile).build();
    when(jobProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);
    when(jobProfileEntityRepository.save(isA(JobProfileEntity.class))).thenReturn(entity);
    when(userClient.getUserById(isA(String.class))).thenReturn(user);

    mockMvc.perform(MockMvcRequestBuilders
      .put("/data-export/job-profiles/" + jobProfile.getId().toString())
        .headers(defaultHeaders())
        .content(asJsonString(jobProfile))
        .contentType(APPLICATION_JSON))
        .andExpect(status().isNoContent());

    verify(jobProfileEntityRepository).save(isA(JobProfileEntity.class));
  }

  @Test
  @SneakyThrows
  void putDefaultJobProfileTest() {
    var jobProfile = new JobProfile();
    jobProfile.setId(UUID.randomUUID());
    jobProfile.setDefault(true);
    jobProfile.setName("jobProfile");
    jobProfile.setMappingProfileId(UUID.randomUUID());

    var entity = JobProfileEntity.builder().id(jobProfile.getId()).jobProfile(jobProfile).build();
    when(jobProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);
    when(jobProfileEntityRepository.save(isA(JobProfileEntity.class))).thenReturn(entity);

    mockMvc.perform(MockMvcRequestBuilders
        .put("/data-export/job-profiles/" + jobProfile.getId().toString())
        .headers(defaultHeaders())
        .content(asJsonString(jobProfile)))
        .andExpect(status().isForbidden());

    verify(jobProfileEntityRepository, times(0)).save(isA(JobProfileEntity.class));
  }
}
