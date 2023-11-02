package org.folio.service.manager.export;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.impl.StorageTestSuite.mockPort;
import static org.folio.util.ErrorCode.ERROR_DUPLICATE_SRS_RECORD;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.clients.ConsortiaClient;
import org.folio.clients.InventoryClient;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.export.ExportService;
import org.folio.service.loader.LoadResult;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.manager.export.strategy.AbstractExportStrategy;
import org.folio.service.manager.export.strategy.InstanceExportStrategyImpl;
import org.folio.service.mapping.converter.InventoryRecordConverterService;
import org.folio.service.mapping.converter.SrsRecordConverterService;
import org.folio.service.profiles.mappingprofile.MappingProfileService;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InstanceExportStrategyUnitTest {
  private static final int LIMIT = 50;
  private static final String DEFAULT_INSTANCE_MAPPING_PROFILE_ID = "25d81cbe-9686-11ea-bb37-0242ac130002";

  @Mock
  private RecordLoaderService recordLoaderService;
  @Mock
  private ExportService exportService;
  @Mock
  private SrsRecordConverterService srsRecordService;
  @Mock
  private InventoryRecordConverterService inventoryRecordService;
  @Mock
  private ErrorLogService errorLogService;
  @Mock
  private MappingProfileService mappingProfileService;
  @Mock
  private ConsortiaClient consortiaClient;
  @InjectMocks
  private InstanceExportStrategyImpl instanceExportManager = Mockito.spy(new InstanceExportStrategyImpl());
  @Mock
  private InventoryClient inventoryClient;

  @Captor
  private ArgumentCaptor<MappingProfile> mappingProfileCaptor;

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @Order(1)
  void exportBlocking_shouldPassExportFor_1000_UnderlyingSrsRecords(boolean isCentralTenant) {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(1000).collect(Collectors.toList());
    SrsLoadResult marcLoadResult = Mockito.mock(SrsLoadResult.class);
    LoadResult loadResult = Mockito.mock(LoadResult.class);
    if (isCentralTenant) {
      Mockito.when(consortiaClient.getCentralTenantId(any())).thenReturn("central");
    } else {
      Mockito.when(consortiaClient.getCentralTenantId(any())).thenReturn("");
    }
    Mockito.when(marcLoadResult.getIdsWithoutSrs()).thenReturn(Collections.singletonList(UUID.randomUUID().toString()));
    Mockito.when(loadResult.getNotFoundEntitiesUUIDs()).thenReturn(Collections.singletonList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadMarcRecordsBlocking(anyList(), eq(AbstractExportStrategy.EntityType.INSTANCE), anyString(), any(OkapiConnectionParams.class))).thenReturn(marcLoadResult);
    Mockito.when(recordLoaderService.loadInventoryInstancesBlocking(anyCollection(), anyString(), any(OkapiConnectionParams.class), eq(LIMIT))).thenReturn(loadResult);
    Mockito.when(mappingProfileService.getDefaultInstanceMappingProfile(any(OkapiConnectionParams.class))).thenReturn(Future.succeededFuture(new MappingProfile()));
    Mockito.when(srsRecordService.transformSrsRecords(any(MappingProfile.class), anyList(), anyString(), any(OkapiConnectionParams.class), any(AbstractExportStrategy.EntityType.class))).thenReturn(
      Pair.of(Collections.emptyList(), 0));
    Mockito.when(inventoryRecordService.transformInstanceRecords(anyList(), anyString(), any(MappingProfile.class), any(OkapiConnectionParams.class))).thenReturn(
      Pair.of(Collections.emptyList(), 0));
    boolean isLast = true;
    FileDefinition fileExportDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    MappingProfile mappingProfile = new MappingProfile().withRecordTypes(Collections.singletonList(RecordType.SRS));
    // when
    ExportPayload exportPayload = new ExportPayload(identifiers, isLast, fileExportDefinition, okapiConnectionParams, "jobExecutionId", mappingProfile);
    instanceExportManager.export(exportPayload, Promise.promise());
    // then
    Mockito.verify(recordLoaderService, Mockito.times(20)).loadMarcRecordsBlocking(anyList(), eq(AbstractExportStrategy.EntityType.INSTANCE), anyString(), any(OkapiConnectionParams.class));
    Mockito.verify(recordLoaderService, Mockito.times(isCentralTenant ? 2 : 1)).loadInventoryInstancesBlocking(anyList(), anyString(), any(OkapiConnectionParams.class), eq(LIMIT));
    Mockito.verify(exportService, Mockito.times(1)).exportSrsRecord(any(Pair.class), any(ExportPayload.class));
    Mockito.verify(inventoryRecordService, Mockito.times(1)).transformInstanceRecords(anyList(), anyString(), any(MappingProfile.class), any(OkapiConnectionParams.class));
    Mockito.verify(exportService, Mockito.times(1)).postExport(any(FileDefinition.class), anyString());
    Mockito.verify(errorLogService).populateUUIDsNotFoundErrorLog(anyString(), anyList(), anyString());
  }

  @Test
  @Order(2)
  void exportBlocking_shouldThrowServerErrorException_whenDefaultMappingProfileNotFound() {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(1000).collect(Collectors.toList());
    SrsLoadResult marcLoadResult = Mockito.mock(SrsLoadResult.class);
    Mockito.when(marcLoadResult.getIdsWithoutSrs()).thenReturn(Collections.singletonList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadMarcRecordsBlocking(anyList(), eq(AbstractExportStrategy.EntityType.INSTANCE), anyString(), any(OkapiConnectionParams.class))).thenReturn(marcLoadResult);
    Mockito.when(mappingProfileService.getDefaultInstanceMappingProfile(any(OkapiConnectionParams.class))).thenReturn(Future.failedFuture(new NotFoundException()));
    Mockito.when(srsRecordService.transformSrsRecords(any(MappingProfile.class), anyList(), anyString(), any(OkapiConnectionParams.class), any(AbstractExportStrategy.EntityType.class))).thenReturn(
      Pair.of(Collections.emptyList(), 0));
    boolean isLast = true;
    FileDefinition fileExportDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    MappingProfile mappingProfile =
      new MappingProfile().withRecordTypes(Collections.singletonList(RecordType.SRS));
    ExportPayload exportPayload = new ExportPayload(identifiers, isLast, fileExportDefinition, okapiConnectionParams, "jobExecutionId", mappingProfile);

    Assertions.assertThrows(ServiceException.class, () -> {
      // when
      instanceExportManager.export(exportPayload, Promise.promise());
    });

    // then
    Mockito.verify(recordLoaderService, Mockito.times(20)).loadMarcRecordsBlocking(anyList(), eq(AbstractExportStrategy.EntityType.INSTANCE), anyString(), any(OkapiConnectionParams.class));
    Mockito.verify(exportService, Mockito.times(1)).exportSrsRecord(any(Pair.class), any(ExportPayload.class));
    Mockito.verify(errorLogService).saveGeneralError(eq(ErrorCode.DEFAULT_MAPPING_PROFILE_NOT_FOUND.getCode()), anyString(), anyString());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @Order(3)
  void exportBlocking_shouldPassExportFor_generatedRecordsOnTheFlyOnly(boolean isCentralTenant) {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(1000).collect(Collectors.toList());
    LoadResult loadResult = Mockito.mock(LoadResult.class);
    if (isCentralTenant) {
      Mockito.when(consortiaClient.getCentralTenantId(any())).thenReturn("central");
    } else {
      Mockito.when(consortiaClient.getCentralTenantId(any())).thenReturn("");
    }
    Mockito.when(loadResult.getNotFoundEntitiesUUIDs()).thenReturn(Collections.singletonList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadInventoryInstancesBlocking(anyCollection(), anyString(), any(OkapiConnectionParams.class), eq(LIMIT))).thenReturn(loadResult);
    Mockito.when(inventoryRecordService.transformInstanceRecords(anyList(), anyString(), any(MappingProfile.class), any(OkapiConnectionParams.class))).thenReturn(
      Pair.of(Collections.emptyList(), 0));
    boolean isLast = true;
    FileDefinition fileExportDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    MappingProfile mappingProfile =
      new MappingProfile().withRecordTypes(Collections.singletonList(RecordType.INSTANCE));
    // when
    ExportPayload exportPayload = new ExportPayload(identifiers, isLast, fileExportDefinition, okapiConnectionParams, "jobExecutionId", mappingProfile);
    instanceExportManager.export(exportPayload, Promise.promise());
    // then
    Mockito.verify(recordLoaderService, Mockito.times(isCentralTenant ? 40 : 20)).loadInventoryInstancesBlocking(anyList(), anyString(), any(OkapiConnectionParams.class), eq(LIMIT));
    Mockito.verify(inventoryRecordService, Mockito.times(1)).transformInstanceRecords(anyList(), anyString(), any(MappingProfile.class), any(OkapiConnectionParams.class));
    Mockito.verify(exportService, Mockito.times(1)).postExport(any(FileDefinition.class), anyString());
    Mockito.verify(errorLogService).populateUUIDsNotFoundErrorLog(anyString(), anyList(), anyString());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @Order(4)
  void exportBlocking_shouldGenerateRecordOnTheFlyByDefaultRules_andHoldingsAndItemTransformations_whenUnderlyingSrsRecordsMissing(boolean isCentralTenant) {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(1000).collect(Collectors.toList());
    SrsLoadResult marcLoadResult = Mockito.mock(SrsLoadResult.class);
    if (isCentralTenant) {
      Mockito.when(consortiaClient.getCentralTenantId(any())).thenReturn("central");
    } else {
      Mockito.when(consortiaClient.getCentralTenantId(any())).thenReturn("");
    }
    Mockito.when(marcLoadResult.getIdsWithoutSrs()).thenReturn(Collections.singletonList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadMarcRecordsBlocking(anyList(), eq(AbstractExportStrategy.EntityType.INSTANCE), anyString(), any(OkapiConnectionParams.class))).thenReturn(marcLoadResult);
    LoadResult loadResult = Mockito.mock(LoadResult.class);
    Mockito.when(recordLoaderService.loadInventoryInstancesBlocking(anyCollection(), anyString(), any(OkapiConnectionParams.class), eq(LIMIT))).thenReturn(loadResult);
    Mockito.when(srsRecordService.transformSrsRecords(any(MappingProfile.class), anyList(), anyString(), any(OkapiConnectionParams.class), any(AbstractExportStrategy.EntityType.class))).thenReturn(
      Pair.of(Collections.emptyList(), 0));
    Mockito.when(inventoryRecordService.transformInstanceRecords(anyList(), anyString(), any(MappingProfile.class), any(OkapiConnectionParams.class))).thenReturn(
      Pair.of(Collections.emptyList(), 0));
    MappingProfile defaultMappingProfile = new MappingProfile()
      .withId(DEFAULT_INSTANCE_MAPPING_PROFILE_ID)
      .withRecordTypes(Arrays.asList(RecordType.INSTANCE));
    Mockito.when(mappingProfileService.getDefaultInstanceMappingProfile(any(OkapiConnectionParams.class))).thenReturn(Future.succeededFuture(defaultMappingProfile));
    boolean isLast = true;
    FileDefinition fileExportDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    Transformations holdingsTransformations = new Transformations()
      .withRecordType(RecordType.HOLDINGS)
      .withTransformation("holdingsTransformationsValue")
      .withPath("holdingsFieldPath")
      .withFieldId("holdingsFieldId");
    Transformations itemTransformations = new Transformations()
      .withRecordType(RecordType.ITEM)
      .withTransformation("itemTransformationsValue")
      .withPath("itemFieldPath")
      .withFieldId("itemFieldId");
    MappingProfile mappingProfile = new MappingProfile()
      .withRecordTypes(Arrays.asList(RecordType.SRS, RecordType.HOLDINGS, RecordType.ITEM))
      .withTransformations(Arrays.asList(holdingsTransformations, itemTransformations));
    ExportPayload exportPayload = new ExportPayload(identifiers, isLast, fileExportDefinition, okapiConnectionParams, "jobExecutionId", mappingProfile);

    //when
    instanceExportManager.export(exportPayload, Promise.promise());

    // then
    Mockito.verify(inventoryRecordService, Mockito.times(1)).transformInstanceRecords(anyList(), anyString(), mappingProfileCaptor.capture(), any(OkapiConnectionParams.class));
    MappingProfile actualMappingProfile = mappingProfileCaptor.getValue();
    assertEquals(DEFAULT_INSTANCE_MAPPING_PROFILE_ID, actualMappingProfile.getId());
    assertThat(actualMappingProfile.getRecordTypes(), hasItems(RecordType.HOLDINGS, RecordType.ITEM));
    assertThat(actualMappingProfile.getTransformations(), hasItems(holdingsTransformations, itemTransformations));
  }

  @Test
  @Order(5)
  void exportBlocking_shouldSaveAllDuplicateSRS() {
    // given
    Map<String, String> headers = new HashMap<>();
    headers.put(OKAPI_HEADER_TENANT, "TENANT_ID");
    headers.put("x-okapi-url", "http://localhost:" + mockPort);
    var okapiConnectionParams = new OkapiConnectionParams(headers);
    var instanceId = UUID.randomUUID().toString();
    var jobExecutionId = UUID.randomUUID().toString();
    var instance = new JsonObject().put("id", instanceId).put("hrid", "0111").put("title", "Test Title");
    var srsAssociated = List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    MappingProfile mappingProfile = new MappingProfile().withRecordTypes(Collections.singletonList(RecordType.SRS));
    List<String> identifiers = List.of(instanceId);
    FileDefinition fileExportDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    ExportPayload exportPayload = new ExportPayload(identifiers, true, fileExportDefinition, okapiConnectionParams,
      jobExecutionId, mappingProfile);
    SrsLoadResult srsLoadResult = new SrsLoadResult();
    srsLoadResult.setIdsWithoutSrs(List.of());
    srsLoadResult.setUnderlyingMarcRecords(List.of(
      new JsonObject().put("recordId", srsAssociated.get(0)).put("externalIdsHolder", new JsonObject().put("instanceId", instanceId)),
      new JsonObject().put("recordId", srsAssociated.get(1)).put("externalIdsHolder", new JsonObject().put("instanceId", instanceId))));
    when(srsRecordService.transformSrsRecords(any(MappingProfile.class), anyList(), anyString(), any(OkapiConnectionParams.class),
      any(AbstractExportStrategy.EntityType.class))).thenReturn(Pair.of(Collections.emptyList(), 0));
    when(recordLoaderService.loadMarcRecordsBlocking(anyList(), eq(AbstractExportStrategy.EntityType.INSTANCE), anyString(),
      any(OkapiConnectionParams.class))).thenReturn(srsLoadResult);
    when(inventoryClient.getInstanceById(jobExecutionId, instanceId, okapiConnectionParams))
      .thenReturn(instance);

    // when
    instanceExportManager.export(exportPayload, Promise.promise());

    // then
    verify(errorLogService).saveWithAffectedRecord(instance, format(ERROR_DUPLICATE_SRS_RECORD.getDescription(), instance.getString("hrid"),
      join(", ", srsAssociated)), ERROR_DUPLICATE_SRS_RECORD.getCode(), jobExecutionId, okapiConnectionParams);
  }

}
