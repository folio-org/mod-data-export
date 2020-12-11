package org.folio.service.profiles.mappingprofile;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.HttpStatus;
import org.folio.clients.UsersClient;
import org.folio.dao.MappingProfileDao;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.MappingProfileCollection;
import org.folio.rest.jaxrs.model.TransformationField;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.transformationfields.TransformationFieldsService;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Implementation of the MappingProfileService, calls MappingProfileDao to access MappingProfile metadata.
 */
@Service
public class MappingProfileServiceImpl implements MappingProfileService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String DEFAULT_MAPPING_PROFILE_ID = "25d81cbe-9686-11ea-bb37-0242ac130002";

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
    if (DEFAULT_MAPPING_PROFILE_ID.equals(mappingProfile.getId())) {
      throw new ServiceException(HttpStatus.HTTP_FORBIDDEN, "Editing of default mapping profile is forbidden");
    }
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
  public Future<MappingProfile> getById(String mappingProfileId, String tenantId) {
    return mappingProfileDao.getById(mappingProfileId, tenantId)
      .compose(optionalMappingProfile -> {
        if (optionalMappingProfile.isPresent()) {
          return succeededFuture(optionalMappingProfile.get());
        } else {
          String errorMessage = String.format("Mapping profile not found with id %s", mappingProfileId);
          LOGGER.error(errorMessage);
          return failedFuture(new NotFoundException(errorMessage));
        }
      });
  }

  @Override
  public Future<MappingProfile> getDefault(String tenantId) {
    return getById(DEFAULT_MAPPING_PROFILE_ID, tenantId);
  }

  @Override
  public Future<Boolean> deleteById(String mappingProfileId, String tenantId) {
    if (DEFAULT_MAPPING_PROFILE_ID.equals(mappingProfileId)) {
      throw new ServiceException(HttpStatus.HTTP_FORBIDDEN, "Deletion of default mapping profile is forbidden");
    }
    return mappingProfileDao.delete(mappingProfileId, tenantId);
  }

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
      return failedFuture(ex);
    }
    return succeededFuture();
  }

  private Optional<TransformationField> getTransformationField(List<TransformationField> transformationFields, Transformations transformation) {
    Optional<TransformationField> transformationFieldOptional = transformationFields.stream()
      .filter(transformationField -> transformation.getFieldId().equals(transformationField.getFieldId()))
      .findFirst();
    if (transformationFieldOptional.isEmpty()) {
      throw new ServiceException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY, String.format("Transformation doesn't exist by provided fieldId: %s", transformation.getFieldId()));
    }
    return transformationFieldOptional;
  }


}
