package org.folio.service.profiles.mappingprofile;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import java.util.Arrays;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.HttpStatus;
import org.folio.clients.UsersClient;
import org.folio.dao.MappingProfileDao;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.MappingProfileCollection;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.TransformationField;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.transformationfields.TransformationFieldsService;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Implementation of the MappingProfileService, calls MappingProfileDao to access MappingProfile metadata.
 */
@Service
public class MappingProfileServiceImpl implements MappingProfileService {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());
  private static final String DEFAULT_INSTANCE_MAPPING_PROFILE_ID = "25d81cbe-9686-11ea-bb37-0242ac130002";
//  private static final String DEFAULT_HOLDINGS_MAPPING_PROFILE_ID = "1ef7d0ac-f0a8-42b5-bbbb-c7e249009c13";

  @Autowired
  private MappingProfileDao mappingProfileDao;
  @Autowired
  private UsersClient usersClient;
  @Autowired
  private TransformationFieldsService transformationFieldsService;

  @Override
  public Future<MappingProfileCollection> get(String query, int offset, int limit, String tenantId) {
    return mappingProfileDao.get(query, offset, limit, tenantId);
  }

  @Override
  public Future<MappingProfile> save(MappingProfile mappingProfile, OkapiConnectionParams params) {
    if (mappingProfile.getId() == null) {
      mappingProfile.setId(UUID.randomUUID().toString());
    }
    validateProfileRecordTypes(mappingProfile);
    Promise<MappingProfile> mappingProfilePromise = Promise.promise();
    if (mappingProfile.getMetadata() != null && mappingProfile.getMetadata().getCreatedByUserId() != null) {
      usersClient.getUserInfoAsync(mappingProfile.getMetadata().getCreatedByUserId(), params)
        .onComplete(userInfoAr -> {
          if (userInfoAr.succeeded()) {
            mappingProfile.withUserInfo(userInfoAr.result());
          }
          mappingProfileDao.save(mappingProfile, params.getTenantId())
            .onSuccess(mappingProfilePromise::complete)
            .onFailure(mappingProfilePromise::fail);
        });
    } else {
      return mappingProfileDao.save(mappingProfile, params.getTenantId());
    }
    return mappingProfilePromise.future();
  }

  @Override
  public Future<MappingProfile> update(MappingProfile mappingProfile, OkapiConnectionParams params) {
    Promise<MappingProfile> mappingProfilePromise = Promise.promise();
//    String newId = mappingProfile.getId();
//    if (DEFAULT_INSTANCE_MAPPING_PROFILE_ID.equals(newId) ||
//      DEFAULT_HOLDINGS_MAPPING_PROFILE_ID.equals(newId)) {
//      throw new ServiceException(HttpStatus.HTTP_FORBIDDEN, "Editing of default mapping profile is forbidden");
//    }
    validateProfileRecordTypes(mappingProfile);
    if (mappingProfile.getMetadata() != null && isNotEmpty(mappingProfile.getMetadata().getUpdatedByUserId())) {
      usersClient.getUserInfoAsync(mappingProfile.getMetadata().getUpdatedByUserId(), params)
        .onComplete(userInfoAr -> {
          if (userInfoAr.succeeded()) {
            mappingProfile.withUserInfo(userInfoAr.result());
          }
          mappingProfileDao.update(mappingProfile, params.getTenantId())
            .onSuccess(mappingProfilePromise::complete)
            .onFailure(mappingProfilePromise::fail);
        });
    } else {
      return mappingProfileDao.update(mappingProfile, params.getTenantId());
    }
    return mappingProfilePromise.future();
  }

  @Override
  public Future<MappingProfile> getById(String mappingProfileId, OkapiConnectionParams params) {
    return mappingProfileDao.getById(mappingProfileId, params.getTenantId())
      .compose(optionalMappingProfile -> {
        if (optionalMappingProfile.isPresent()) {
          return updateTransformationFields(optionalMappingProfile.get(), params);
        } else {
          String errorMessage = String.format("Mapping profile not found with id %s", mappingProfileId);
          LOGGER.error(errorMessage);
          return Future.failedFuture(new NotFoundException(errorMessage));
        }
      });
  }

  private Future<MappingProfile> updateTransformationFields(MappingProfile mappingProfile, OkapiConnectionParams params) {
    return transformationFieldsService.getTransformationFields(params).compose(fieldCollection -> {
      boolean updateMappingProfile = false;
      for (Transformations profileTransformation : mappingProfile.getTransformations()) {
        for (TransformationField transformationField : fieldCollection.getTransformationFields()) {
          if (transformationField.getReferenceDataValue() != null) {
            String profileTransformationPath = profileTransformation.getPath();
            String transformationFieldPath = transformationField.getPath();
            String profileTransformationId = profileTransformation.getFieldId();
            String transformationFieldId = transformationField.getFieldId();
            if (profileTransformationPath.equals(transformationFieldPath) && !profileTransformationId.equals(transformationFieldId)) {
              profileTransformation.setFieldId(transformationFieldId);
              updateMappingProfile = true;
            }
          }
        }
      }
      return updateMappingProfile
        ? mappingProfileDao.update(mappingProfile, params.getTenantId())
        : Future.succeededFuture(mappingProfile);
    });
  }

  @Override
  public Future<MappingProfile> getDefault(OkapiConnectionParams params) {
    return getById(DEFAULT_INSTANCE_MAPPING_PROFILE_ID, params);
  }

  @Override
  public Future<Boolean> deleteById(String mappingProfileId, String tenantId) {
    if (DEFAULT_INSTANCE_MAPPING_PROFILE_ID.equals(mappingProfileId)) {
      throw new ServiceException(HttpStatus.HTTP_FORBIDDEN, "Deletion of default mapping profile is forbidden");
    }
    return mappingProfileDao.delete(mappingProfileId, tenantId);
  }

  /**
   * Due to many validation methods are presented at this class,
   * they should vbe moved to a separate place.
   */
  @Override
  public Future<Void> validate(MappingProfile mappingProfile, OkapiConnectionParams params) {
    Promise<Void> promise = Promise.promise();
    if (CollectionUtils.isNotEmpty(mappingProfile.getTransformations())) {
      transformationFieldsService.validateTransformations(mappingProfile.getTransformations())
        .compose(v -> transformationFieldsService.getTransformationFields(params)
          .compose(transformationFieldCollection -> validateTransformation(mappingProfile, transformationFieldCollection)))
        .onSuccess(v -> promise.complete())
        .onFailure(promise::fail);
    } else {
      promise.complete();
    }
    return promise.future();
  }

  private Future<Void> validateTransformation(MappingProfile mappingProfile, org.folio.rest.jaxrs.model.TransformationFieldCollection transformationFieldCollection) {
    List<TransformationField> transformationFields = transformationFieldCollection.getTransformationFields();
    try {
      mappingProfile.getTransformations().stream()
        .filter(transformation -> {
          String fieldId = transformation.getFieldId();
          if (StringUtils.isBlank(fieldId)) {
            throw new ServiceException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY, "Field id is missing for mapping profile transformation");
          }
          return true;
        }).forEach(transformation -> {
        Optional<TransformationField> transformationFieldOptional = getTransformationField(transformationFields, transformation);
        TransformationField transformationField = transformationFieldOptional.get();
        TransformationField.RecordType expectedRecordType = transformationField.getRecordType();
        if (Objects.isNull(transformation.getRecordType()) || !transformation.getRecordType().toString().equals(expectedRecordType.toString())) {
          throw new ServiceException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY, String.format("Transformation record type is missing or incorrect according to provided fieldId: %s, " +
            "expected record type: %s", transformation.getFieldId(), expectedRecordType));
        }
      });
    } catch (Exception ex) {
      return Future.failedFuture(ex);
    }
    return Future.succeededFuture();
  }

  private Optional<TransformationField> getTransformationField(List<TransformationField> transformationFields, Transformations transformation) {
    Optional<TransformationField> transformationFieldOptional = transformationFields.stream()
      .filter(transformationField -> transformation.getFieldId().equals(transformationField.getFieldId()))
      .findFirst();
    if (transformationFieldOptional.isEmpty()) {
      throw new ServiceException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY, String.format("Transformation doesn't exist for provided fieldId: %s", transformation.getFieldId()));
    }
    return transformationFieldOptional;
  }

  private void validateProfileRecordTypes(MappingProfile mappingProfile) {
    if (mappingProfile.getRecordTypes().containsAll(Arrays.asList(RecordType.INSTANCE, RecordType.SRS))) {
      LOGGER.error("SRS record type cannot be combined together with INSTANCE record type in mapping profile");
      throw new ServiceException(HttpStatus.HTTP_FORBIDDEN, ErrorCode.INVALID_SRS_MAPPING_PROFILE_RECORD_TYPE);
    }
  }

  public static boolean isDefault(String mappingProfileId) {
    return DEFAULT_INSTANCE_MAPPING_PROFILE_ID.equals(mappingProfileId);
  }

}
