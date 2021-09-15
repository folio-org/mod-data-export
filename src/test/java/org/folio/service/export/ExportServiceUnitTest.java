package org.folio.service.export;

import io.vertx.core.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.util.Lists;
import org.folio.TestUtil;
import org.folio.clients.InventoryClient;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.service.file.storage.FileStorage;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.manager.export.ExportPayload;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.marc4j.MarcException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.folio.util.ErrorCode.ERROR_MARC_RECORD_CANNOT_BE_CONVERTED;
import static org.folio.util.ErrorCode.ERROR_MARC_RECORD_CONTAINS_CONTROL_CHARACTERS;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class ExportServiceUnitTest {
  private static final String JOB_EXECUTION_ID = "jobExecutionId";
  private static final String RECORD_CONTENT = "record content";
  private static final String FILE_DEFINITION_ID = "file definition id";
  private static final String TENANT = "tenant";
  private static final String RECORDS_RESPONSE_JSON_FILE_PATH = "mockData/srs/get_records_response.json";
  private static final String LONG_SRS_RECORD_JSON_FILE_PATH = "mockData/srs/srs_record_content_117000_characters_length.json";
  private static final String INSTANCES_RESPONSE_JSON_FILE_PATH = "clients/inventory/get_instances_response.json";
  private static final String INSTANCE_ID = "6666df22-5df3-412b-b9cc-cbdddb928e93";

  @Mock
  private FileStorage fileStorage;
  @Mock
  private ExportStorageService exportStorageService;
  @Mock
  private ErrorLogService errorLogService;
  @Mock
  private InventoryClient inventoryClient;
  @InjectMocks
  private ExportService exportService = new LocalFileSystemExportService();

  private final FileDefinition fileDefinition = new FileDefinition();
  private final String jobExecutionId = UUID.randomUUID().toString();
  private final OkapiConnectionParams params = new OkapiConnectionParams();
  private final ExportPayload exportPayload = new ExportPayload(emptyList(), true, fileDefinition, params, jobExecutionId, new MappingProfile());

  @Test
  void shouldPassExportFor_1_SrsRecord() {
    // given
    String response = TestUtil.readFileContentFromResources(RECORDS_RESPONSE_JSON_FILE_PATH);
    String jsonRecord = new JsonObject(response).getJsonArray("sourceRecords").getJsonObject(0).toString();
    Pair<List<String>, Integer> marcRecordsToExport = MutablePair.of(Collections.singletonList(jsonRecord), 0);

    when(fileStorage.saveFileDataBlocking(any(byte[].class), any(FileDefinition.class))).thenReturn(fileDefinition);
    // when
    exportService.exportSrsRecord(marcRecordsToExport, exportPayload);
    // then
    Mockito.verify(fileStorage, Mockito.times(1)).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
    assertEquals(0, marcRecordsToExport.getValue().intValue());
  }

  @Test
  void shouldPassExportFor_NULL_SrsRecords() {
    // given
    Pair<List<String>, Integer> marcRecordsToExport = Pair.of(null, 0);
    // when
    exportService.exportSrsRecord(marcRecordsToExport, exportPayload);
    // then
    Mockito.verify(fileStorage, Mockito.times(0)).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
    assertEquals(0, marcRecordsToExport.getValue().intValue());
  }

  @Test
  void shouldSaveErrorLog_whenJsonCannotBeConvertedToMarcRecordBecauseOfLargeSize() {
    //given
    String record = TestUtil.readFileContentFromResources(LONG_SRS_RECORD_JSON_FILE_PATH);
    String stringJson = TestUtil.readFileContentFromResources(INSTANCES_RESPONSE_JSON_FILE_PATH);
    JsonObject instances = new JsonObject(stringJson);
    JsonObject instance = instances.getJsonArray("instances").getJsonObject(0);
    Pair<List<String>, Integer> marcRecordsToExport = MutablePair.of(Collections.singletonList(record), 0);

    when(inventoryClient.getInstancesByIds(Collections.singletonList(INSTANCE_ID), jobExecutionId, params, 1)).thenReturn(Optional.of(instances));
    //when
    exportService.exportSrsRecord(marcRecordsToExport, exportPayload);
    //then
    Mockito.verify(errorLogService, Mockito.times(1)).saveWithAffectedRecord(eq(instance), eq(ERROR_MARC_RECORD_CANNOT_BE_CONVERTED.getCode()), eq(jobExecutionId), any(MarcException.class), eq(params));
    assertEquals(1, marcRecordsToExport.getValue().intValue());
  }

  @Test
  void shouldSaveErrorLog_whenJsonCannotBeConvertedToMarcRecordBecauseOfControlCharacters() {
    //given
    String record = "{\"leader\":\"00000nam a2200000 a 4500\",\"fields\":[{\"001\":\"in00000000011\"},{\"005\":\"20210728150129.6\007\"},{\"008\":\"950721s1996    nyua     b    000 0 eng  \007\"},{\"010\":{\"subfields\":[{\"a\":\"   95036522\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"015\":{\"subfields\":[{\"a\":\"GB9678521\"},{\"2\":\"bnb\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"019\":{\"subfields\":[{\"a\":\"36260797\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"020\":{\"subfields\":[{\"a\":\"0471309400\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"020\":{\"subfields\":[{\"a\":\"9780471309406\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"035\":{\"subfields\":[{\"a\":\"(OCoLC)32923445\"},{\"z\":\"(OCoLC)36260797\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"035\":{\"subfields\":[{\"a\":\"(OCoLC)ocm32923445\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"040\":{\"subfields\":[{\"a\":\"DLC\"},{\"b\":\"eng\"},{\"c\":\"DLC\"},{\"d\":\"UKM\"},{\"d\":\"BAKER\"},{\"d\":\"BTCTA\"},{\"d\":\"YDXCP\"},{\"d\":\"DEBBG\"},{\"d\":\"OCL\"},{\"d\":\"BDX\"},{\"d\":\"OCLCF\"},{\"d\":\"OCLCQ\"},{\"d\":\"OCLCO\"},{\"d\":\"TXM\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"041\":{\"subfields\":[{\"a\":\"rus\"},{\"b\":\"eng\"}],\"ind1\":\"0\",\"ind2\":\" \"}},{\"049\":{\"subfields\":[{\"a\":\"TXMM\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"050\":{\"subfields\":[{\"a\":\"PG2117\"},{\"b\":\".G59 1996\"}],\"ind1\":\"0\",\"ind2\":\"0\"}},{\"066\":{\"subfields\":[{\"c\":\"(N\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"082\":{\"subfields\":[{\"a\":\"491.786/421\"},{\"2\":\"20\"}],\"ind1\":\"0\",\"ind2\":\"0\"}},{\"092\":{\"subfields\":[{\"a\":\"491.786\"},{\"b\":\"G5651\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"245\":{\"subfields\":[{\"a\":\"The Golden Age :\"},{\"b\":\"readings in Russian literature of the nineteenth century /\"},{\"c\":\"[compiled by] Sandra F. Rosengrant, Elena D. Lifschitz.\"}],\"ind1\":\"0\",\"ind2\":\"4\"}},{\"260\":{\"subfields\":[{\"a\":\"New York :\"},{\"b\":\"J. Wiley,\"},{\"c\":\"©1996.\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"300\":{\"subfields\":[{\"a\":\"v, 375 p. :\"},{\"b\":\"ill. ;\"},{\"c\":\"24 cm.\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"500\":{\"subfields\":[{\"a\":\"UTF8  leader encoded with UTF8 record\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"504\":{\"subfields\":[{\"a\":\"Includes bibliographical references.\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"650\":{\"subfields\":[{\"a\":\"Russian language\"},{\"v\":\"Readers.\"}],\"ind1\":\" \",\"ind2\":\"0\"}},{\"650\":{\"subfields\":[{\"a\":\"Russian literature\"},{\"y\":\"19th century.\"}],\"ind1\":\" \",\"ind2\":\"0\"}},{\"923\":{\"subfields\":[{\"g\":\"very_long_volume very_long_volume very_long_volume very_long_volume very_long_volume very_long_volume very_long_volume \"},{\"3\":\"ho00000000002\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"945\":{\"subfields\":[{\"a\":\"491.786\"},{\"b\":\"G5651\"},{\"g\":\"1\"},{\"i\":\"33082017611558\"},{\"j\":\"0\"},{\"l\":\"w3   \"},{\"o\":\"-\"},{\"p\":\"$76.68\"},{\"q\":\"-\"},{\"r\":\"-\"},{\"s\":\"-  \"},{\"t\":\"0\"},{\"u\":\"3\"},{\"v\":\"0\"},{\"w\":\"0\"},{\"x\":\"3\"},{\"y\":\".i21033298\"},{\"z\":\"05-22-15\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"994\":{\"subfields\":[{\"a\":\"C0\"},{\"b\":\"TXM\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"998\":{\"subfields\":[{\"a\":\"w\"},{\"b\":\"05-26-15\"},{\"c\":\"m\"},{\"d\":\"a  \"},{\"e\":\"-\"},{\"f\":\"eng\"},{\"g\":\"nyu\"},{\"h\":\"4\"},{\"i\":\"1\"}],\"ind1\":\" \",\"ind2\":\" \"}},{\"999\":{\"subfields\":[{\"s\":\"b26859f1-8c79-47b3-b047-1ecde668492f\"},{\"i\":\"6666df22-5df3-412b-b9cc-cbdddb928e93\"}],\"ind1\":\"f\",\"ind2\":\"f\"}}]}\n";
    String stringJson = TestUtil.readFileContentFromResources(INSTANCES_RESPONSE_JSON_FILE_PATH);
    JsonObject instances = new JsonObject(stringJson);
    JsonObject instance = instances.getJsonArray("instances").getJsonObject(0);
    Pair<List<String>, Integer> marcRecordsToExport = MutablePair.of(Collections.singletonList(record), 0);

    when(inventoryClient.getInstancesByIds(Collections.singletonList(INSTANCE_ID), jobExecutionId, params, 1)).thenReturn(Optional.of(instances));
    //when
    exportService.exportSrsRecord(marcRecordsToExport, exportPayload);
    //then
    Mockito.verify(errorLogService, Mockito.times(1)).saveWithAffectedRecord(eq(instance), eq(ERROR_MARC_RECORD_CONTAINS_CONTROL_CHARACTERS.getCode()), eq(jobExecutionId), any(MarcException.class), eq(params));
    assertEquals(1, marcRecordsToExport.getValue().intValue());
  }

  @Test
  void shouldPassExportFor_1_InventoryRecord() {
    // given
    String inventoryRecord = "testRecord";
    FileDefinition fileDefinition = new FileDefinition();
    when(fileStorage.saveFileDataBlocking(any(byte[].class), any(FileDefinition.class))).thenReturn(fileDefinition);
    // when
    exportService.exportInventoryRecords(Collections.singletonList(inventoryRecord), fileDefinition, TENANT);
    // then
    Mockito.verify(fileStorage, Mockito.times(1)).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
  }

  @Test
  void shouldNotStoreMarcInFile_whenInventoryRecordIsEmpty() {
    // given
    String inventoryRecord = StringUtils.EMPTY;
    FileDefinition fileDefinition = new FileDefinition();
    // when
    exportService.exportInventoryRecords(Collections.singletonList(inventoryRecord), fileDefinition, TENANT);
    // then
    Mockito.verify(fileStorage, never()).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
  }

  @Test
  void shouldNotStoreMarcInFile_whenSRSRecordIsEmpty() {
    // given
    String inventoryRecord = StringUtils.EMPTY;
    Pair<List<String>, Integer> marcRecordsToExport = MutablePair.of(Collections.singletonList(inventoryRecord), 0);

    // when
    exportService.exportSrsRecord(marcRecordsToExport, exportPayload);
    // then
    Mockito.verify(fileStorage, never()).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
    assertEquals(0, marcRecordsToExport.getValue().intValue());
  }


  @Test
  void shouldPassExportFor_NULL_InventoryRecords() {
    // given
    List<String> inventoryRecords = null;
    FileDefinition fileDefinition = new FileDefinition();
    // when
    exportService.exportInventoryRecords(inventoryRecords, fileDefinition, TENANT);
    // then
    Mockito.verify(fileStorage, Mockito.times(0)).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
  }

  @Test
  void shouldPopulateErrorLog_whenExportInventoryRecordFails() {
    // given
    List<String> inventoryRecords = Lists.newArrayList(RECORD_CONTENT);
    FileDefinition fileDefinition = new FileDefinition();
    fileDefinition.setJobExecutionId(JOB_EXECUTION_ID);
    when(fileStorage.saveFileDataBlocking(any(byte[].class), any(FileDefinition.class))).thenThrow(RuntimeException.class);
    // when
    exportService.exportInventoryRecords(inventoryRecords, fileDefinition, TENANT);
    // then
    verify(errorLogService).saveGeneralError(ErrorCode.ERROR_SAVING_RECORD_TO_FILE.getCode(), JOB_EXECUTION_ID, TENANT);
  }

  @Test
  void postExport_shouldStoreFile() {
    // given
    FileDefinition fileDefinition = new FileDefinition().withSourcePath("generatedBinaryFile.mrc");
    // when
    exportService.postExport(fileDefinition, TENANT);
    // then
    Mockito.verify(exportStorageService, Mockito.times(1)).storeFile(any(FileDefinition.class), anyString());
  }

  @Test
  void postExport_shouldNotStoreFileFor_Null_FileDefinition() {
    // given
    FileDefinition fileDefinition = null;
    // when
    Assertions.assertThrows(ServiceException.class, () -> {
      exportService.postExport(fileDefinition, TENANT);
    });

    verify(errorLogService).saveGeneralError(ErrorCode.INVALID_EXPORT_FILE_DEFINITION.getCode(), "", TENANT);
  }

  @Test
  void postExport_shouldNotStoreFileFor_Null_SourcePath() {
    // given
    FileDefinition fileDefinition = new FileDefinition()
      .withJobExecutionId(JOB_EXECUTION_ID)
      .withId(FILE_DEFINITION_ID)
      .withSourcePath(null);
    // when
    Assertions.assertThrows(ServiceException.class, () -> {
      exportService.postExport(fileDefinition, TENANT);
    });
    // then
    verify(errorLogService).saveGeneralErrorWithMessageValues(ErrorCode.INVALID_EXPORT_FILE_DEFINITION_ID.getCode(), Arrays.asList(FILE_DEFINITION_ID), JOB_EXECUTION_ID, TENANT);
  }
}
