package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.Errors;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.MappingProfileCollection;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.ParametersInner;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.UserInfo;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.exception.mapping.profile.DefaultMappingProfileException;
import org.folio.dataexp.exception.mapping.profile.MappingProfileTransformationEmptyException;
import org.folio.dataexp.exception.mapping.profile.MappingProfileTransformationPatternException;
import org.folio.dataexp.repository.MappingProfileEntityCqlRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MappingProfileService {

  private static final Pattern TRANSFORMATION_PATTERN = Pattern.compile("((\\d{3}([\\s]|[\\d]|[a-zA-Z]){2}(\\$([a-zA-Z]|[\\d]{1,2}))?)|(^$))");
  private static final String ERROR_CODE = "javax.validation.constraints.Pattern.message";
  private static final String ERROR_VALIDATION_PARAMETER_KEY_PATTERN = "transformations[%s].transformation";
  private static final String ERROR_VALIDATION_MESSAGE_PATTERN = "must match \\\"%s\\\"";
  private static final String TRANSFORMATION_ITEM_EMPTY_VALUE_MESSAGE = "Transformations for fields with item record type cannot be empty. Please provide a value.";

  private final FolioExecutionContext folioExecutionContext;
  private final MappingProfileEntityRepository mappingProfileEntityRepository;
  private final MappingProfileEntityCqlRepository mappingProfileEntityCqlRepository;
  private final UserClient userClient;

  public void deleteMappingProfileById(UUID mappingProfileId) {
    var mappingProfileEntity = mappingProfileEntityRepository.getReferenceById(mappingProfileId);
    if (Boolean.TRUE.equals(mappingProfileEntity.getMappingProfile().getDefault()))
      throw new DefaultMappingProfileException("Deletion of default mapping profile is forbidden");
    mappingProfileEntityRepository.deleteById(mappingProfileId);
  }

  public MappingProfileEntity getMappingProfileById(UUID mappingProfileId) {
    return mappingProfileEntityRepository.getReferenceById(mappingProfileId);
  }

  public MappingProfileCollection getMappingProfiles(String query, Integer offset, Integer limit) {
    if (StringUtils.isEmpty(query)) query = "(cql.allRecords=1)";
    var mappingProfilesPage = mappingProfileEntityCqlRepository.findByCql(query, OffsetRequest.of(offset, limit));
    var mappingProfiles = mappingProfilesPage.stream().map(MappingProfileEntity::getMappingProfile).toList();
    var mappingProfileCollection = new MappingProfileCollection();
    mappingProfileCollection.setMappingProfiles(mappingProfiles);
    mappingProfileCollection.setTotalRecords((int) mappingProfilesPage.getTotalElements());
    return mappingProfileCollection;
  }

  public MappingProfile postMappingProfile(MappingProfile mappingProfile) {
    var userId = folioExecutionContext.getUserId().toString();
    var user = userClient.getUserById(userId);
    var userInfo = new UserInfo();
    userInfo.setFirstName(user.getPersonal().getFirstName());
    userInfo.setLastName(user.getPersonal().getLastName());
    userInfo.setUserName(user.getUsername());
    mappingProfile.setUserInfo(userInfo);

    var metaData = new Metadata();
    metaData.createdByUserId(userId);
    metaData.updatedByUserId(userId);
    var current = new Date();
    metaData.createdDate(current);
    metaData.updatedDate(current);

    metaData.createdByUsername(user.getUsername());
    metaData.updatedByUsername(user.getUsername());
    mappingProfile.setMetadata(metaData);

    validateMappingProfileTransformations(mappingProfile);

    var saved = mappingProfileEntityRepository.save(MappingProfileEntity.fromMappingProfile(mappingProfile));
    return saved.getMappingProfile();
  }

  public void putMappingProfile(UUID mappingProfileId, MappingProfile mappingProfile) {
    var mappingProfileEntity = mappingProfileEntityRepository.getReferenceById(mappingProfileId);
    if (Boolean.TRUE.equals(mappingProfileEntity.getMappingProfile().getDefault())) {
      throw new DefaultMappingProfileException("Editing of default mapping profile is forbidden");
    }

    var userId = folioExecutionContext.getUserId().toString();
    var user = userClient.getUserById(userId);

    var userInfo = new UserInfo();
    userInfo.setFirstName(user.getPersonal().getFirstName());
    userInfo.setLastName(user.getPersonal().getLastName());
    userInfo.setUserName(user.getUsername());
    mappingProfile.setUserInfo(userInfo);

    var metadataOfExistingMappingProfile = mappingProfileEntity.getMappingProfile().getMetadata();

    var metadata = Metadata.builder()
      .createdDate(metadataOfExistingMappingProfile.getCreatedDate())
      .updatedDate(new Date())
      .createdByUserId(metadataOfExistingMappingProfile.getCreatedByUserId())
      .updatedByUserId(userId)
      .createdByUsername(metadataOfExistingMappingProfile.getCreatedByUsername())
      .updatedByUsername(user.getUsername())
      .build();

    mappingProfile.setMetadata(metadata);

    mappingProfileEntityRepository.save(MappingProfileEntity.fromMappingProfile(mappingProfile));
  }

  private void validateMappingProfileTransformations(MappingProfile mappingProfile) {
    var transformations = mappingProfile.getTransformations();
    var parameters = new ArrayList<ParametersInner>();
    for (int i = 0; i < transformations.size(); i++) {
      var transformation = transformations.get(i);
      var matcher = TRANSFORMATION_PATTERN.matcher(transformation.getTransformation());
      if (!matcher.matches()) {
        var parameter = ParametersInner.builder()
          .key(String.format(ERROR_VALIDATION_PARAMETER_KEY_PATTERN, i))
          .value(transformation.getTransformation()).build();
        parameters.add(parameter);
      }
    }
    if (!parameters.isEmpty()) {
      var errors = new Errors();
      for (var parameter : parameters) {
        var errorItem = new org.folio.dataexp.domain.dto.Error();
        errorItem.setCode(ERROR_CODE);
        errorItem.type("1");
        errorItem.message(String.format(ERROR_VALIDATION_MESSAGE_PATTERN, TRANSFORMATION_PATTERN));
        errors.addErrorsItem(errorItem);
        errorItem.setParameters(List.of(parameter));
      }
      errors.setTotalRecords(errors.getErrors().size());
      throw new MappingProfileTransformationPatternException("Mapping profile validation exception", errors);
    }
    for (var transformation : transformations) {
      if (StringUtils.isEmpty(transformation.getTransformation()) && transformation.getRecordType() == RecordTypes.ITEM) {
        throw new MappingProfileTransformationEmptyException(TRANSFORMATION_ITEM_EMPTY_VALUE_MESSAGE);
      }
    }
  }
}
