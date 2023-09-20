package org.folio.dataexp.controllers;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.User;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.repository.MappingProfileEntityCqlRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MappingProfileControllerTest extends BaseDataExportInitializer {

  @MockBean
  private MappingProfileEntityRepository mappingProfileEntityRepository;
  @MockBean
  private MappingProfileEntityCqlRepository mappingProfileEntityCqlRepository;
  @MockBean
  private UserClient userClient;

  @Test
  @SneakyThrows
  void deleteMappingProfileByIdTest() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(false);
    mappingProfile.setName("mappingProfile");

    var entity = MappingProfileEntity.builder().id(mappingProfile.getId()).mappingProfile(mappingProfile).build();

    when(mappingProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);

    mockMvc.perform(MockMvcRequestBuilders
        .delete("/data-export/mapping-profiles/" + mappingProfile.getId().toString())
        .headers(defaultHeaders()))
      .andExpect(status().isNoContent());

    verify(mappingProfileEntityRepository).deleteById(isA(UUID.class));
  }

  @Test
  @SneakyThrows
  void deleteDefaultMappingProfileByIdTest() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");

    var entity = MappingProfileEntity.builder().id(mappingProfile.getId()).mappingProfile(mappingProfile).build();

    when(mappingProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);

    mockMvc.perform(MockMvcRequestBuilders
        .delete("/data-export/mapping-profiles/" + mappingProfile.getId().toString())
        .headers(defaultHeaders()))
      .andExpect(status().isForbidden());

    verify(mappingProfileEntityRepository, times(0)).deleteById(isA(UUID.class));
  }

  @Test
  @SneakyThrows
  void getMappingProfileByIdTest() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");

    var entity = MappingProfileEntity.builder().id(mappingProfile.getId()).mappingProfile(mappingProfile).build();

    when(mappingProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);

    mockMvc.perform(MockMvcRequestBuilders
        .get("/data-export/mapping-profiles/" + mappingProfile.getId().toString())
        .headers(defaultHeaders()))
      .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  void getMappingProfiles() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");

    var entity = MappingProfileEntity.builder().id(mappingProfile.getId()).mappingProfile(mappingProfile).build();
    PageImpl<MappingProfileEntity> page = new PageImpl<>(List.of(entity));

    when(mappingProfileEntityCqlRepository.findByCQL(isA(String.class), isA(OffsetRequest.class))).thenReturn(page);

    mockMvc.perform(MockMvcRequestBuilders
        .get("/data-export/mapping-profiles?query=query")
        .headers(defaultHeaders()))
      .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  void postMappingProfileTest() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");
    var user = new User();
    user.setPersonal(new User.Personal());

    var entity = MappingProfileEntity.builder().id(mappingProfile.getId()).mappingProfile(mappingProfile).build();
    when(mappingProfileEntityRepository.save(isA(MappingProfileEntity.class))).thenReturn(entity);
    when(userClient.getUserById(isA(String.class))).thenReturn(user);

    mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/mapping-profiles")
        .headers(defaultHeaders())
        .content(asJsonString(mappingProfile)))
      .andExpect(status().isCreated());

    verify(mappingProfileEntityRepository).save(isA(MappingProfileEntity.class));
  }


  @Test
  @SneakyThrows
  void putMappingProfileTest() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(false);
    mappingProfile.setName("mappingProfile");
    mappingProfile.setMetadata(new Metadata().createdDate(new Date()));
    var user = new User();
    user.setPersonal(new User.Personal());

    var entity = MappingProfileEntity.builder().id(mappingProfile.getId()).mappingProfile(mappingProfile).build();
    when(mappingProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);
    when(mappingProfileEntityRepository.save(isA(MappingProfileEntity.class))).thenReturn(entity);
    when(userClient.getUserById(isA(String.class))).thenReturn(user);

    mockMvc.perform(MockMvcRequestBuilders
        .put("/data-export/mapping-profiles/" + mappingProfile.getId().toString())
        .headers(defaultHeaders())
        .content(asJsonString(mappingProfile)))
      .andExpect(status().isNoContent());

    verify(mappingProfileEntityRepository).save(isA(MappingProfileEntity.class));
  }

  @Test
  @SneakyThrows
  void putDefaultMappingProfileTest() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");

    var entity = MappingProfileEntity.builder().id(mappingProfile.getId()).mappingProfile(mappingProfile).build();
    when(mappingProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);
    when(mappingProfileEntityRepository.save(isA(MappingProfileEntity.class))).thenReturn(entity);

    mockMvc.perform(MockMvcRequestBuilders
        .put("/data-export/mapping-profiles/" + mappingProfile.getId().toString())
        .headers(defaultHeaders())
        .content(asJsonString(mappingProfile)))
      .andExpect(status().isForbidden());

    verify(mappingProfileEntityRepository, times(0)).save(isA(MappingProfileEntity.class));
  }
}
