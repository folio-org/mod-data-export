package org.folio.service.manager.export;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.service.export.ExportService;
import org.folio.service.loader.InventoryLoadResult;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.logs.ErrorLogService;
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
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
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
class ExportManagerUnitTest {
  private static final int LIMIT = 50;
  private static final String DEFAULT_MAPPING_PROFILE_ID = "25d81cbe-9686-11ea-bb37-0242ac130002";

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
  @InjectMocks
  private ExportManagerImpl exportManager = Mockito.spy(new ExportManagerImpl());

  @Test
  @Order(1)
  void exportBlocking_shouldPassExportFor_1000_UnderlyingSrsRecords() {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(1000).collect(Collectors.toList());
    SrsLoadResult marcLoadResult = Mockito.mock(SrsLoadResult.class);
    InventoryLoadResult inventoryLoadResult = Mockito.mock(InventoryLoadResult.class);
    Mockito.when(marcLoadResult.getInstanceIdsWithoutSrs()).thenReturn(Collections.singletonList(UUID.randomUUID().toString()));
    Mockito.when(inventoryLoadResult.getNotFoundInstancesUUIDs()).thenReturn(Collections.singletonList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadMarcRecordsBlocking(anyList(), anyString(), any(OkapiConnectionParams.class))).thenReturn(marcLoadResult);
    Mockito.when(recordLoaderService.loadInventoryInstancesBlocking(anyCollection(), anyString(), any(OkapiConnectionParams.class), eq(LIMIT))).thenReturn(inventoryLoadResult);
    Mockito.when(mappingProfileService.getById(eq(DEFAULT_MAPPING_PROFILE_ID), anyString())).thenReturn(Future.succeededFuture(new MappingProfile()));
    boolean isLast = true;
    FileDefinition fileExportDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    MappingProfile mappingProfile =
        new MappingProfile().withRecordTypes(Collections.singletonList(RecordType.SRS));
    // when
    ExportPayload exportPayload = new ExportPayload(identifiers, isLast, fileExportDefinition, okapiConnectionParams, "jobExecutionId", mappingProfile);
    exportManager.exportBlocking(exportPayload, Promise.promise());
    // then
    Mockito.verify(recordLoaderService, Mockito.times(20)).loadMarcRecordsBlocking(anyList(), anyString(), any(OkapiConnectionParams.class));
    Mockito.verify(recordLoaderService, Mockito.times(1)).loadInventoryInstancesBlocking(anyList(), anyString(), any(OkapiConnectionParams.class), eq(LIMIT));
    Mockito.verify(exportService, Mockito.times(1)).exportSrsRecord(anyList(), any(FileDefinition.class));
    Mockito.verify(inventoryRecordService, Mockito.times(1)).transformInventoryRecords(anyList(), anyString(), any(MappingProfile.class), any(OkapiConnectionParams.class));
    Mockito.verify(exportService, Mockito.times(1)).postExport(any(FileDefinition.class), anyString());
    Mockito.verify(errorLogService).populateUUIDsNotFoundErrorLog(anyString(), anyList(), anyString());
  }

  @Test
  @Order(2)
  void exportBlocking_shouldThrowServerErrorException_whenDefaultMappingProfileNotFound() {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(1000).collect(Collectors.toList());
    SrsLoadResult marcLoadResult = Mockito.mock(SrsLoadResult.class);
    InventoryLoadResult inventoryLoadResult = Mockito.mock(InventoryLoadResult.class);
    Mockito.when(marcLoadResult.getInstanceIdsWithoutSrs()).thenReturn(Collections.singletonList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadMarcRecordsBlocking(anyList(), anyString(), any(OkapiConnectionParams.class))).thenReturn(marcLoadResult);
    Mockito.when(mappingProfileService.getById(eq(DEFAULT_MAPPING_PROFILE_ID), anyString())).thenReturn(Future.failedFuture(new NotFoundException()));
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
      exportManager.exportBlocking(exportPayload, Promise.promise());
    });

    // then
    Mockito.verify(recordLoaderService, Mockito.times(20)).loadMarcRecordsBlocking(anyList(), anyString(), any(OkapiConnectionParams.class));
    Mockito.verify(exportService, Mockito.times(1)).exportSrsRecord(anyList(), any(FileDefinition.class));
    Mockito.verify(errorLogService).saveGeneralError(eq(ErrorCode.DEFAULT_MAPPING_PROFILE_NOT_FOUND.getDescription()), anyString(), anyString());
  }

  @Test
  @Order(3)
  void exportBlocking_shouldPassExportFor_generatedRecordsOnTheFlyOnly() {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(1000).collect(Collectors.toList());
    InventoryLoadResult inventoryLoadResult = Mockito.mock(InventoryLoadResult.class);
    Mockito.when(inventoryLoadResult.getNotFoundInstancesUUIDs()).thenReturn(Collections.singletonList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadInventoryInstancesBlocking(anyCollection(), anyString(), any(OkapiConnectionParams.class), eq(LIMIT))).thenReturn(inventoryLoadResult);
    boolean isLast = true;
    FileDefinition fileExportDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    MappingProfile mappingProfile =
      new MappingProfile().withRecordTypes(Collections.singletonList(RecordType.INSTANCE));
    // when
    ExportPayload exportPayload = new ExportPayload(identifiers, isLast, fileExportDefinition, okapiConnectionParams, "jobExecutionId", mappingProfile);
    exportManager.exportBlocking(exportPayload, Promise.promise());
    // then
    Mockito.verify(recordLoaderService, Mockito.times(20)).loadInventoryInstancesBlocking(anyList(), anyString(), any(OkapiConnectionParams.class), eq(LIMIT));
    Mockito.verify(inventoryRecordService, Mockito.times(1)).transformInventoryRecords(anyList(), anyString(), any(MappingProfile.class), any(OkapiConnectionParams.class));
    Mockito.verify(exportService, Mockito.times(1)).postExport(any(FileDefinition.class), anyString());
    Mockito.verify(errorLogService).populateUUIDsNotFoundErrorLog(anyString(), anyList(), anyString());
  }

}
