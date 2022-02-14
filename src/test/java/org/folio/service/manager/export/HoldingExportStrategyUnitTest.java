package org.folio.service.manager.export;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.service.export.ExportService;
import org.folio.service.loader.LoadResult;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.manager.export.strategy.AbstractExportStrategy;
import org.folio.service.manager.export.strategy.HoldingExportStrategyImpl;
import org.folio.service.mapping.converter.InventoryRecordConverterService;
import org.folio.service.mapping.converter.SrsRecordConverterService;
import org.folio.util.OkapiConnectionParams;
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

import io.vertx.core.Promise;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HoldingExportStrategyUnitTest {

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
  @InjectMocks
  private HoldingExportStrategyImpl holdingExportManager = Mockito.spy(new HoldingExportStrategyImpl());

  @Test
  @Order(1)
  void exportBlockingShouldPassExportFor_1000_Records_whenPartOfRecordsDontHaveSrsRecord() {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(1000).collect(Collectors.toList());
    SrsLoadResult marcLoadResult = Mockito.mock(SrsLoadResult.class);
    LoadResult loadResult = Mockito.mock(LoadResult.class);
    Mockito.when(marcLoadResult.getIdsWithoutSrs()).thenReturn(Collections.singletonList(UUID.randomUUID().toString()));
    Mockito.when(loadResult.getNotFoundEntitiesUUIDs()).thenReturn(Collections.singletonList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadMarcRecordsBlocking(anyList(), eq(AbstractExportStrategy.EntityType.HOLDING), anyString(), any(OkapiConnectionParams.class))).thenReturn(marcLoadResult);
    Mockito.when(recordLoaderService.getHoldingsById(anyList(), anyString(), any(OkapiConnectionParams.class), anyInt())).thenReturn(loadResult);
    Mockito.when(srsRecordService.transformSrsRecords(any(MappingProfile.class), anyList(), anyString(), any(OkapiConnectionParams.class), eq(AbstractExportStrategy.EntityType.HOLDING))).thenReturn(
      Pair.of(Collections.emptyList(), 0));
    Mockito.when(inventoryRecordService.transformHoldingRecords(anyList(), anyString(), any(MappingProfile.class), any(OkapiConnectionParams.class))).thenReturn(
      Pair.of(Collections.emptyList(), 0));
    boolean isLast = true;
    FileDefinition fileExportDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc")
      .withJobExecutionId(UUID.randomUUID().toString());
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    MappingProfile mappingProfile = new MappingProfile().withRecordTypes(Collections.singletonList(RecordType.SRS));
    // when
    ExportPayload exportPayload = new ExportPayload(identifiers, isLast, fileExportDefinition, okapiConnectionParams, "jobExecutionId", mappingProfile);
    holdingExportManager.export(exportPayload, Promise.promise());
    // then
    Mockito.verify(recordLoaderService, Mockito.times(20)).loadMarcRecordsBlocking(anyList(), eq(AbstractExportStrategy.EntityType.HOLDING), anyString(), any(OkapiConnectionParams.class));
    Mockito.verify(recordLoaderService, Mockito.times(1)).getHoldingsById(anyList(), anyString(), any(OkapiConnectionParams.class), anyInt());
    Mockito.verify(exportService, Mockito.times(1)).exportSrsRecord(any(Pair.class), any(ExportPayload.class));
    Mockito.verify(inventoryRecordService, Mockito.times(1)).transformHoldingRecords(anyList(), anyString(), any(MappingProfile.class), any(OkapiConnectionParams.class));
    Mockito.verify(exportService, Mockito.times(1)).postExport(any(FileDefinition.class), anyString());
    Mockito.verify(errorLogService).populateUUIDsNotFoundErrorLog(anyString(), anyList(), anyString());
  }

}
