package org.folio.dataexp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.Errors;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.Transformations;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    when(mappingProfileEntityCqlRepository.findByCql(isA(String.class), isA(OffsetRequest.class))).thenReturn(page);

    mockMvc.perform(MockMvcRequestBuilders
        .get("/data-export/mapping-profiles?query=query")
        .headers(defaultHeaders()))
      .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  void postMappingProfileTest() {
    var transformation = new Transformations();
    transformation.setFieldId("holdings.callnumber");
    transformation.setPath("$.holdings[*].callNumber");
    transformation.setRecordType(RecordTypes.HOLDINGS);
    transformation.setTransformation("900  $a");
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");
    mappingProfile.setTransformations(List.of(transformation));
    mappingProfile.setFieldsSuppression("902");
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
  void postMappingProfileTestIfTransformationsNotMatchPattern() {
    var transformation1 = new Transformations();
    transformation1.setFieldId("holdings.callnumber");
    transformation1.setPath("$.holdings[*].callNumber");
    transformation1.setRecordType(RecordTypes.HOLDINGS);
    transformation1.setTransformation("902q $aaa");

    var transformation2 = new Transformations();
    transformation2.setFieldId("holdings.callnumber");
    transformation2.setPath("$.holdings[*].callNumber");
    transformation2.setRecordType(RecordTypes.HOLDINGS);
    transformation2.setTransformation("902q $bbb");

    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");
    mappingProfile.setTransformations(List.of(transformation1, transformation2));
    var user = new User();
    user.setPersonal(new User.Personal());

    when(userClient.getUserById(isA(String.class))).thenReturn(user);

    var result = mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/mapping-profiles")
        .headers(defaultHeaders())
        .content(asJsonString(mappingProfile)))
      .andExpect(status().isUnprocessableEntity()).andReturn();

    var response = result.getResponse().getContentAsString();
    var mapper = new ObjectMapper();

    var errors = mapper.readValue(response, Errors.class);
    assertEquals(2, errors.getErrors().size());

    var error = errors.getErrors().get(0);

    assertEquals("must match \\\"((\\d{3}([\\s]|[\\d]|[a-zA-Z]){2}(\\$([a-zA-Z]|[\\d]{1,2}))?)|(^$))\\\"", error.getMessage());
    assertEquals(1, error.getParameters().size());
    assertEquals("transformations[0].transformation", error.getParameters().get(0).getKey());
    assertEquals("902q $aaa", error.getParameters().get(0).getValue());

    error = errors.getErrors().get(1);

    assertEquals("must match \\\"((\\d{3}([\\s]|[\\d]|[a-zA-Z]){2}(\\$([a-zA-Z]|[\\d]{1,2}))?)|(^$))\\\"", error.getMessage());
    assertEquals(1, error.getParameters().size());
    assertEquals("transformations[1].transformation", error.getParameters().get(0).getKey());
    assertEquals("902q $bbb", error.getParameters().get(0).getValue());
  }

  @Test
  @SneakyThrows
  void postMappingProfileTestIfItemTransformationEmpty() {
    var transformation = new Transformations();
    transformation.setFieldId("holdings.callnumber");
    transformation.setPath("$.holdings[*].callNumber");
    transformation.setRecordType(RecordTypes.ITEM);
    transformation.setTransformation(StringUtils.EMPTY);
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");
    mappingProfile.setTransformations(List.of(transformation));
    var user = new User();
    user.setPersonal(new User.Personal());

    when(userClient.getUserById(isA(String.class))).thenReturn(user);

    var result = mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/mapping-profiles")
        .headers(defaultHeaders())
        .content(asJsonString(mappingProfile)))
      .andExpect(status().isUnprocessableEntity()).andReturn();

    var response = result.getResponse().getContentAsString();
    var expected = "Transformations for fields with item record type cannot be empty. Please provide a value.";
    assertEquals(expected, response);
  }

  @Test
  @SneakyThrows
  void postMappingProfileTestIfItemTransformationEmptyAndTransformationsNotMatchPattern() {
    var transformation1 = new Transformations();
    transformation1.setFieldId("holdings.callnumber");
    transformation1.setPath("$.holdings[*].callNumber");
    transformation1.setRecordType(RecordTypes.ITEM);
    transformation1.setTransformation(StringUtils.EMPTY);

    var transformation2 = new Transformations();
    transformation2.setFieldId("holdings.callnumber");
    transformation2.setPath("$.holdings[*].callNumber");
    transformation2.setRecordType(RecordTypes.HOLDINGS);
    transformation2.setTransformation("902q $bbb");

    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");
    mappingProfile.setTransformations(List.of(transformation1, transformation2));
    var user = new User();
    user.setPersonal(new User.Personal());

    when(userClient.getUserById(isA(String.class))).thenReturn(user);

    var result = mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/mapping-profiles")
        .headers(defaultHeaders())
        .content(asJsonString(mappingProfile)))
      .andExpect(status().isUnprocessableEntity()).andReturn();

    var response = result.getResponse().getContentAsString();
    var mapper = new ObjectMapper();

    var errors = mapper.readValue(response, Errors.class);
    assertEquals(1, errors.getErrors().size());

    var error = errors.getErrors().get(0);

    assertEquals("must match \\\"((\\d{3}([\\s]|[\\d]|[a-zA-Z]){2}(\\$([a-zA-Z]|[\\d]{1,2}))?)|(^$))\\\"", error.getMessage());
    assertEquals(1, error.getParameters().size());
    assertEquals("transformations[1].transformation", error.getParameters().get(0).getKey());
    assertEquals("902q $bbb", error.getParameters().get(0).getValue());
  }

  @Test
  @SneakyThrows
  void postMappingProfileIfSuppressionNotMatchTest() {
    var transformation = new Transformations();
    transformation.setFieldId("holdings.callnumber");
    transformation.setPath("$.holdings[*].callNumber");
    transformation.setRecordType(RecordTypes.HOLDINGS);
    transformation.setTransformation("900  $a");
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");
    mappingProfile.setTransformations(List.of(transformation));
    mappingProfile.setFieldsSuppression("897 , 90");
    var user = new User();
    user.setPersonal(new User.Personal());

    var entity = MappingProfileEntity.builder().id(mappingProfile.getId()).mappingProfile(mappingProfile).build();
    when(mappingProfileEntityRepository.save(isA(MappingProfileEntity.class))).thenReturn(entity);
    when(userClient.getUserById(isA(String.class))).thenReturn(user);

    var result = mockMvc.perform(MockMvcRequestBuilders
        .post("/data-export/mapping-profiles")
        .headers(defaultHeaders())
        .content(asJsonString(mappingProfile)))
      .andExpect(status().isUnprocessableEntity()).andReturn();

    var response = result.getResponse().getContentAsString();
    var mapper = new ObjectMapper();

    var errors = mapper.readValue(response, Errors.class);
    assertEquals(1, errors.getErrors().size());

    var error = errors.getErrors().get(0);

    assertEquals("must match \\\"^\\d{3}$\\\"", error.getMessage());
    assertEquals(1, error.getParameters().size());
    assertEquals("suppressionFields[1]", error.getParameters().get(0).getKey());
    assertEquals("90", error.getParameters().get(0).getValue());
  }

  @Test
  @SneakyThrows
  void putMappingProfileTest() {
    var transformation = new Transformations();
    transformation.setFieldId("holdings.callnumber");
    transformation.setPath("$.holdings[*].callNumber");
    transformation.setRecordType(RecordTypes.HOLDINGS);
    transformation.setTransformation("900  $a");
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(false);
    mappingProfile.setName("mappingProfile");
    mappingProfile.setTransformations(List.of(transformation));
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
    var transformation = new Transformations();
    transformation.setFieldId("holdings.callnumber");
    transformation.setPath("$.holdings[*].callNumber");
    transformation.setRecordType(RecordTypes.HOLDINGS);
    transformation.setTransformation("900  $a");
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");
    mappingProfile.setTransformations(List.of(transformation));

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
