package org.folio.service.manager.export;

import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.export.ExportService;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.mapping.MappingService;
import org.folio.service.mapping.convertor.SrsRecordConvertorService;
import org.folio.util.OkapiConnectionParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import io.vertx.core.json.JsonObject;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class ExportManagerUnitTest {
  private static final int LIMIT = 20;
  private static final String INSTANCE_ID = "c8b50e3f-0446-429c-960e-03774b88223f";
  private static final String HOLDINGS_ID = "65cb2bf0-d4c2-4886-8ad0-b76f1ba75d61";
  private static final String ITEM_ID_1 = "0b96a642-5e7f-452d-9cae-9cee66c9a892";
  private static final String ITEM_ID_2 = "5b31ec8c-95a7-4b91-95cc-b551a74b91ca";

  @Mock
  private RecordLoaderService recordLoaderService;
  @Mock
  private ExportService exportService;
  @Mock
  private MappingService mappingService;
  @Mock
  private SrsRecordConvertorService srsRecordService;
  @InjectMocks
  private ExportManagerImpl exportManager = Mockito.spy(new ExportManagerImpl());

  @Captor
  private ArgumentCaptor<List> instancesCaptor;

  @Test
  void exportBlocking_shouldPassExportFor_1000_Records() {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(1000).collect(Collectors.toList());
    SrsLoadResult marcLoadResult = Mockito.mock(SrsLoadResult.class);
    Mockito.when(marcLoadResult.getInstanceIdsWithoutSrs()).thenReturn(Arrays.asList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadMarcRecordsBlocking(anyList(), any(OkapiConnectionParams.class), eq(LIMIT))).thenReturn(marcLoadResult);
    boolean isLast = true;
    FileDefinition fileExportDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    // when
    ExportPayload exportPayload = new ExportPayload(identifiers, isLast, fileExportDefinition, okapiConnectionParams, "jobExecutionId", new MappingProfile());
    exportManager.exportBlocking(exportPayload);
    // then
    Mockito.verify(recordLoaderService, Mockito.times(50)).loadMarcRecordsBlocking(anyList(), any(OkapiConnectionParams.class), eq(LIMIT));
    Mockito.verify(recordLoaderService, Mockito.times(3)).loadInventoryInstancesBlocking(anyList(), any(OkapiConnectionParams.class), eq(LIMIT));
    Mockito.verify(exportService, Mockito.times(1)).exportSrsRecord(anyList(), any(FileDefinition.class));
    Mockito.verify(mappingService, Mockito.times(1)).map(anyList(), any(MappingProfile.class), anyString(), any(OkapiConnectionParams.class));
    Mockito.verify(exportService, Mockito.times(1)).postExport(any(FileDefinition.class), anyString());
  }

  @Test
  void exportBlocking_shouldNotPopulateHoldingsItemsFor_MappingProfileTransformation_whenTransformationsEmpty() {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(10).collect(Collectors.toList());
    SrsLoadResult marcLoadResult = Mockito.mock(SrsLoadResult.class);
    Mockito.when(marcLoadResult.getInstanceIdsWithoutSrs()).thenReturn(Arrays.asList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadMarcRecordsBlocking(anyList(), any(OkapiConnectionParams.class), eq(LIMIT))).thenReturn(marcLoadResult);
    Mockito.when(recordLoaderService.loadInventoryInstancesBlocking(anyList(), any(OkapiConnectionParams.class), eq(LIMIT)))
      .thenReturn(Arrays.asList(new JsonObject().put("id", INSTANCE_ID)));
    boolean isLast = true;
    FileDefinition fileExportDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    MappingProfile mappingProfile = new MappingProfile()
      .withRecordTypes(Arrays.asList(RecordType.INSTANCE));
    // when
    ExportPayload exportPayload = new ExportPayload(identifiers, isLast, fileExportDefinition, okapiConnectionParams, "jobExecutionId", mappingProfile);
    exportManager.exportBlocking(exportPayload);
    // then
    Mockito.verify(mappingService, Mockito.times(1)).map(instancesCaptor.capture(), any(MappingProfile.class), anyString(), any(OkapiConnectionParams.class));
    List<JsonObject> instances = instancesCaptor.getValue();
    assertEquals(INSTANCE_ID, instances.get(0).getJsonObject("instance").getString("id"));
    assertNull(instances.get(0).getJsonArray("holdings"));
    assertNull( instances.get(0).getJsonArray("items"));

  }

  @Test
  void exportBlocking_shouldNotPopulateHoldingsItemsFor_MappingProfileTransformation_whenRecordTypesContainsInstanceOnly() {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(10).collect(Collectors.toList());
    SrsLoadResult marcLoadResult = Mockito.mock(SrsLoadResult.class);
    Mockito.when(marcLoadResult.getInstanceIdsWithoutSrs()).thenReturn(Arrays.asList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadMarcRecordsBlocking(anyList(), any(OkapiConnectionParams.class), eq(LIMIT))).thenReturn(marcLoadResult);
    Mockito.when(recordLoaderService.loadInventoryInstancesBlocking(anyList(), any(OkapiConnectionParams.class), eq(LIMIT)))
      .thenReturn(Arrays.asList(new JsonObject().put("id", INSTANCE_ID)));
    boolean isLast = true;
    FileDefinition fileExportDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    MappingProfile mappingProfile = new MappingProfile()
      .withRecordTypes(Arrays.asList(RecordType.INSTANCE))
      .withTransformations(Arrays.asList(new Transformations()));
    // when
    ExportPayload exportPayload = new ExportPayload(identifiers, isLast, fileExportDefinition, okapiConnectionParams, "jobExecutionId", mappingProfile);
    exportManager.exportBlocking(exportPayload);
    // then
    Mockito.verify(mappingService, Mockito.times(1)).map(instancesCaptor.capture(), any(MappingProfile.class), anyString(), any(OkapiConnectionParams.class));
    List<JsonObject> instances = instancesCaptor.getValue();
    assertEquals(INSTANCE_ID, instances.get(0).getJsonObject("instance").getString("id"));
    assertNull(instances.get(0).getJsonArray("holdings"));
    assertNull( instances.get(0).getJsonArray("items"));

  }

  @Test
  void exportBlocking_shouldNotPopulateItemsFor_MappingProfileTransformation_whenRecordTypesContainsHoldingsOnly() {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(10).collect(Collectors.toList());
    SrsLoadResult marcLoadResult = Mockito.mock(SrsLoadResult.class);
    Mockito.when(marcLoadResult.getInstanceIdsWithoutSrs()).thenReturn(Arrays.asList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadMarcRecordsBlocking(anyList(), any(OkapiConnectionParams.class), eq(LIMIT))).thenReturn(marcLoadResult);
    Mockito.when(recordLoaderService.loadInventoryInstancesBlocking(anyList(), any(OkapiConnectionParams.class), eq(LIMIT)))
      .thenReturn(Arrays.asList(new JsonObject().put("id", INSTANCE_ID)));
    Mockito.when(recordLoaderService.getHoldingsForInstance(eq(INSTANCE_ID), any(OkapiConnectionParams.class)))
      .thenReturn(Arrays.asList(new JsonObject().put("id", HOLDINGS_ID)));
    boolean isLast = true;
    FileDefinition fileExportDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    MappingProfile mappingProfile = new MappingProfile()
      .withRecordTypes(Arrays.asList(RecordType.HOLDINGS))
      .withTransformations(Arrays.asList(new Transformations()));
    // when
    ExportPayload exportPayload = new ExportPayload(identifiers, isLast, fileExportDefinition, okapiConnectionParams, "jobExecutionId", mappingProfile);
    exportManager.exportBlocking(exportPayload);
    // then
    Mockito.verify(mappingService, Mockito.times(1)).map(instancesCaptor.capture(), any(MappingProfile.class), anyString(), any(OkapiConnectionParams.class));
    List<JsonObject> instancesWithHoldings = instancesCaptor.getValue();
    assertEquals(INSTANCE_ID, instancesWithHoldings.get(0).getJsonObject("instance").getString("id"));
    assertEquals(HOLDINGS_ID, instancesWithHoldings.get(0).getJsonArray("holdings").getJsonObject(0).getString("id"));
    assertNull( instancesWithHoldings.get(0).getJsonArray("items"));

  }

  @Test
  void exportBlocking_shouldPopulateHoldingsItemsFor_MappingProfileTransformation_whenRecordTypesContainsHoldingsnAndItem() {
    // given
    List<String> identifiers = Stream.generate(String::new).limit(10).collect(Collectors.toList());
    SrsLoadResult marcLoadResult = Mockito.mock(SrsLoadResult.class);
    Mockito.when(marcLoadResult.getInstanceIdsWithoutSrs()).thenReturn(Arrays.asList(UUID.randomUUID().toString()));
    Mockito.when(recordLoaderService.loadMarcRecordsBlocking(anyList(), any(OkapiConnectionParams.class), eq(LIMIT))).thenReturn(marcLoadResult);
    Mockito.when(recordLoaderService.loadInventoryInstancesBlocking(anyList(), any(OkapiConnectionParams.class), eq(LIMIT)))
      .thenReturn(Arrays.asList(new JsonObject().put("id", INSTANCE_ID)));
    Mockito.when(recordLoaderService.getHoldingsForInstance(eq(INSTANCE_ID), any(OkapiConnectionParams.class)))
      .thenReturn(Arrays.asList(new JsonObject().put("id", HOLDINGS_ID)));
    Mockito.when(recordLoaderService.getAllItemsForHolding(eq(Arrays.asList(HOLDINGS_ID)), any(OkapiConnectionParams.class)))
      .thenReturn(Arrays.asList(new JsonObject().put("id", ITEM_ID_1),
                                new JsonObject().put("id", ITEM_ID_2)));
    boolean isLast = true;
    FileDefinition fileExportDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    MappingProfile mappingProfile = new MappingProfile()
      .withRecordTypes(Arrays.asList(RecordType.HOLDINGS, RecordType.ITEM))
      .withTransformations(Arrays.asList(new Transformations()));
    // when
    ExportPayload exportPayload = new ExportPayload(identifiers, isLast, fileExportDefinition, okapiConnectionParams, "jobExecutionId", mappingProfile);
    exportManager.exportBlocking(exportPayload);
    // then
    Mockito.verify(mappingService, Mockito.times(1)).map(instancesCaptor.capture(), any(MappingProfile.class), anyString(), any(OkapiConnectionParams.class));
    List<JsonObject> instancesWithHoldingsAndItem = instancesCaptor.getValue();
    assertEquals(INSTANCE_ID, instancesWithHoldingsAndItem.get(0).getJsonObject("instance").getString("id"));
    assertEquals(HOLDINGS_ID, instancesWithHoldingsAndItem.get(0).getJsonArray("holdings").getJsonObject(0).getString("id"));
    assertEquals(ITEM_ID_1, instancesWithHoldingsAndItem.get(0).getJsonArray("items").getJsonObject(0).getString("id"));
    assertEquals(ITEM_ID_2, instancesWithHoldingsAndItem.get(0).getJsonArray("items").getJsonObject(1).getString("id"));
  }
}
