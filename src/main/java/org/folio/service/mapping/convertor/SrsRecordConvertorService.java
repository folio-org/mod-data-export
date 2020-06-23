package org.folio.service.mapping.convertor;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.service.mapping.MappingService;
import org.folio.util.OkapiConnectionParams;
import org.marc4j.MarcJsonReader;
import org.marc4j.MarcJsonWriter;
import org.marc4j.MarcReader;
import org.marc4j.MarcWriter;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;
import org.marc4j.marc.impl.SortedMarcFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
public class SrsRecordConvertorService extends RecordConvertor {
  @Autowired
  private MappingService mappingService;

  private SortedMarcFactoryImpl sortedMarcFactory = new SortedMarcFactoryImpl();
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup()
    .lookupClass());

  public List<String> transformSrsRecords(MappingProfile mappingProfile, List<JsonObject> srsRecords, String jobExecutionId,
                                          OkapiConnectionParams connectionParams) {
    if (isTransformationRequired(mappingProfile)) {
      return transformSrsRecord(mappingProfile, srsRecords, jobExecutionId, connectionParams);
    } else {
      return getRecordContent(srsRecords);
    }
  }

  public List<String> transformSrsRecord(MappingProfile mappingProfile, List<JsonObject> srsRecords, String jobExecutionId,
                                         OkapiConnectionParams connectionParams) {
    List<String> marcRecords = new ArrayList<>();
    for (JsonObject srsRecord : srsRecords) {
      // generate record fields by mapping profile
      List<VariableField> mappedFields = getMappedFields(mappingProfile, jobExecutionId, connectionParams, srsRecord);
      //get list of selected fields without transformations
      List<String> selectedFieldIds = mappingService.getFieldIdsWithoutTransformations(mappingProfile, connectionParams);
      // convert srs record to marc and append generated fields
      marcRecords.add(convert(srsRecord.encode(), mappedFields, selectedFieldIds));
    }
    return marcRecords;
  }

  private List<VariableField> getMappedFields(MappingProfile mappingProfile, String jobExecutionId,
                                              OkapiConnectionParams connectionParams, JsonObject srsRecord) {
    List<VariableField> mappedFields = Collections.emptyList();
    JsonObject externalIdsHolder = srsRecord.getJsonObject("externalIdsHolder");
    if (externalIdsHolder != null) {
      String instanceId = externalIdsHolder.getString("instanceId");
      if (isNotBlank(instanceId)) {
        JsonObject holdingsAndItems = new JsonObject();
        fetchHoldingsAndItems(mappingProfile, connectionParams, instanceId, holdingsAndItems);
        LOGGER.debug("Processing mapping for appending to SRS records for instanceID: {}", instanceId);
        mappedFields = mappingService.mapFields(holdingsAndItems, mappingProfile, jobExecutionId, connectionParams);
      }
    }

    return mappedFields;
  }

  public String convert(String jsonRecord, List<VariableField> mappedFields, List<String> selectedFieldIds) {
    MarcReader marcJsonReader = new MarcJsonReader(new ByteArrayInputStream(jsonRecord.getBytes(StandardCharsets.UTF_8)));
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    MarcWriter marcStreamWriter = new MarcJsonWriter(byteArrayOutputStream);
    while (marcJsonReader.hasNext()) {
      Record record = marcJsonReader.next();
      if(CollectionUtils.isNotEmpty(mappedFields) || CollectionUtils.isNotEmpty(selectedFieldIds)) {
        record = getRecordWithTransformationFields(record, mappedFields, selectedFieldIds);
      }
      marcStreamWriter.write(record);
    }
    return byteArrayOutputStream.toString();
  }

  private Record getRecordWithTransformationFields(Record record, List<VariableField> mappedFields, List<String> selectedFieldIds) {
    Record sortedRecord = sortedMarcFactory.newRecord();
    for (VariableField recordField : record.getVariableFields()) {
      if (selectedFieldIds.contains(recordField.getTag())) {
        sortedRecord.addVariableField(recordField);
      }
    }
    for (VariableField generatedField : mappedFields) {
      sortedRecord.addVariableField(generatedField);
    }
    return sortedRecord;
  }

  private List<String> getRecordContent(List<JsonObject> records) {
    return records.parallelStream()
      .map(jo -> jo.getJsonObject("parsedRecord")
        .getJsonObject("content")
        .encode())
      .collect(Collectors.toList());
  }

}
