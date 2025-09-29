package org.folio.dataexp.service;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.domain.dto.User;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.repository.MappingProfileEntityCqlRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.service.validators.MappingProfileValidator;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class MappingProfileServiceTest {
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private MappingProfileEntityRepository mappingProfileEntityRepository;
  @Mock
  private MappingProfileEntityCqlRepository mappingProfileEntityCqlRepository;
  @Mock
  private MappingProfileValidator mappingProfileValidator;
  @Mock
  private UserClient userClient;

  @InjectMocks
  private MappingProfileService mappingProfileService;

  @Test
  void deleteMappingProfileByIdTest() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(false);
    mappingProfile.setName("mappingProfile");
    var entity = MappingProfileEntity.builder().id(mappingProfile.getId())
        .mappingProfile(mappingProfile).build();

    when(mappingProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);

    mappingProfileService.deleteMappingProfileById(mappingProfile.getId());

    verify(mappingProfileEntityRepository).deleteById(isA(UUID.class));
  }

  @Test
  @SneakyThrows
  void getMappingProfileByIdTest() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");

    var entity = MappingProfileEntity.builder().id(mappingProfile.getId())
        .mappingProfile(mappingProfile).build();
    when(mappingProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);

    mappingProfileService.getMappingProfileById(mappingProfile.getId());

    verify(mappingProfileEntityRepository).getReferenceById(isA(UUID.class));
  }

  @Test
  @SneakyThrows
  void getMappingProfiles() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");

    var entity = MappingProfileEntity.builder().id(mappingProfile.getId())
        .mappingProfile(mappingProfile).build();
    PageImpl<MappingProfileEntity> page = new PageImpl<>(List.of(entity));

    when(mappingProfileEntityCqlRepository.findByCql(isA(String.class), isA(OffsetRequest.class)))
        .thenReturn(page);

    mappingProfileService.getMappingProfiles("query", 2, 1);

    verify(mappingProfileEntityCqlRepository).findByCql(isA(String.class),
        isA(OffsetRequest.class));
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
    var user = new User();
    user.setPersonal(new User.Personal());
    var entity = MappingProfileEntity.builder().id(mappingProfile.getId())
        .mappingProfile(mappingProfile).build();

    when(mappingProfileEntityRepository.save(isA(MappingProfileEntity.class))).thenReturn(entity);
    when(userClient.getUserById(isA(String.class))).thenReturn(user);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    mappingProfileService.postMappingProfile(mappingProfile);

    verify(mappingProfileEntityRepository).save(isA(MappingProfileEntity.class));
    verify(mappingProfileValidator).validate(isA(MappingProfile.class));
  }

  @Test
  void putMappingProfileTest() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(false);
    mappingProfile.setName("mappingProfile");
    mappingProfile.setMetadata(new Metadata().createdDate(new Date()));
    var user = new User();
    user.setPersonal(new User.Personal());

    var entity = MappingProfileEntity.builder().id(mappingProfile.getId())
        .mappingProfile(mappingProfile).build();
    when(mappingProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);
    when(mappingProfileEntityRepository.save(isA(MappingProfileEntity.class))).thenReturn(entity);
    when(userClient.getUserById(isA(String.class))).thenReturn(user);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    mappingProfileService.putMappingProfile(mappingProfile.getId(), mappingProfile);

    verify(mappingProfileEntityRepository).save(isA(MappingProfileEntity.class));
    verify(mappingProfileValidator).validate(isA(MappingProfile.class));
  }
}
