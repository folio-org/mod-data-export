package org.folio.service.mapping.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.service.manager.export.strategy.AbstractExportStrategy;
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

import io.vertx.core.json.JsonObject;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
public class SrsRecordConverterService extends RecordConverter {

  @Autowired
  private MappingService mappingService;

  private SortedMarcFactoryImpl sortedMarcFactory = new SortedMarcFactoryImpl();
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup()
    .lookupClass());

  public Pair<List<String>, Integer> transformSrsRecords(MappingProfile mappingProfile, List<JsonObject> srsRecords, String jobExecutionId,
                                                         OkapiConnectionParams connectionParams, AbstractExportStrategy.EntityType entityType) {
    if (entityType == AbstractExportStrategy.EntityType.INSTANCE && isTransformationRequired(mappingProfile)) {
      return transformSrsRecord(mappingProfile, srsRecords, jobExecutionId, connectionParams, entityType);
    } else {
      return MutablePair.of(getRecordContent(srsRecords), 0);
    }
  }

  public Pair<List<String>, Integer>  transformSrsRecord(MappingProfile mappingProfile, List<JsonObject> srsRecords, String jobExecutionId,
                                                         OkapiConnectionParams connectionParams, AbstractExportStrategy.EntityType entityType) {
    List<String> marcRecords = new ArrayList<>();
    int failedCount = 0;
    for (JsonObject srsRecord : srsRecords) {
      // generate record fields by mapping profile
      Pair<List<VariableField>, Integer> mappedFields = Pair.of(Collections.emptyList(), 0);
      switch (entityType) {
        case HOLDING:
          mappedFields = getMappedFieldsByHoldingsId(mappingProfile, jobExecutionId, connectionParams, srsRecord);
          break;
        case INSTANCE:
          mappedFields = getMappedFieldsByInstanceId(mappingProfile, jobExecutionId, connectionParams, srsRecord);
          break;
      }
      // convert srs record to marc and append generated fields
      marcRecords.add(convert(srsRecord.encode(), mappedFields.getKey()));
      failedCount = failedCount + mappedFields.getValue();
    }
    return MutablePair.of(marcRecords, failedCount);
  }

  private Pair<List<VariableField>, Integer> getMappedFieldsByHoldingsId(MappingProfile mappingProfile, String jobExecutionId,
                                                                         OkapiConnectionParams connectionParams, JsonObject srsRecord) {
    Pair<List<VariableField>, Integer> mappedFields = Pair.of(Collections.emptyList(), 0);
    JsonObject externalIdsHolder = srsRecord.getJsonObject("externalIdsHolder");
    if (externalIdsHolder != null) {
      String holdingId = externalIdsHolder.getString("holdingsId");
      String holdingHrId = externalIdsHolder.getString("holdingsHrid");
      if (isNotBlank(holdingId)) {
        JsonObject holdingsAndItems = new JsonObject();
        fetchHoldingsAndItems(mappingProfile, connectionParams, holdingId, holdingHrId, RecordType.HOLDINGS, holdingsAndItems, jobExecutionId);
        LOGGER.debug("Processing mapping for appending to SRS records for holdingId: {}", holdingId);
        mappedFields = mappingService.mapFields(holdingsAndItems, mappingProfile, jobExecutionId, connectionParams);
      }
    }

    return mappedFields;
  }

  private Pair<List<VariableField>, Integer> getMappedFieldsByInstanceId(MappingProfile mappingProfile, String jobExecutionId,
                                                                         OkapiConnectionParams connectionParams, JsonObject srsRecord) {
    Pair<List<VariableField>, Integer> mappedFields = Pair.of(Collections.emptyList(), 0);
    JsonObject externalIdsHolder = srsRecord.getJsonObject("externalIdsHolder");
      if (externalIdsHolder != null) {
      String instanceId = externalIdsHolder.getString("instanceId");
      String instanceHrId = externalIdsHolder.getString("instanceHrid");
      if (isNotBlank(instanceId)) {
        JsonObject holdingsAndItems = new JsonObject();
        fetchHoldingsAndItems(mappingProfile, connectionParams, instanceId, instanceHrId, RecordType.INSTANCE, holdingsAndItems, jobExecutionId);
        LOGGER.debug("Processing mapping for appending to SRS records for instanceID: {}", instanceId);
        mappedFields = mappingService.mapFields(holdingsAndItems, mappingProfile, jobExecutionId, connectionParams);
      }
    }

    return mappedFields;
  }

  public String convert(String jsonRecord, List<VariableField> additionalFields) {
    var byteArrayInputStream = new ByteArrayInputStream(jsonRecord.getBytes(StandardCharsets.UTF_8));
    var byteArrayOutputStream = new ByteArrayOutputStream();
    try (byteArrayInputStream; byteArrayOutputStream) {
      MarcReader marcJsonReader = new MarcJsonReader(byteArrayInputStream);
      MarcWriter marcStreamWriter = new MarcJsonWriter(byteArrayOutputStream);
      while (marcJsonReader.hasNext()) {
        Record record = marcJsonReader.next();
        if (CollectionUtils.isNotEmpty(additionalFields)) {
          record = appendAdditionalFields(record, additionalFields);
        }
        marcStreamWriter.write(record);
      }
      return byteArrayOutputStream.toString();
    } catch (IOException e) {
      return null;
    }
  }

  private Record appendAdditionalFields(Record record, List<VariableField> additionalFields) {
    Record sortedRecord = sortedMarcFactory.newRecord();
    sortedRecord.getLeader().setRecordStatus(record.getLeader().getRecordStatus());
    for (VariableField recordField : record.getVariableFields()) {
      sortedRecord.addVariableField(recordField);
    }
    for (VariableField generatedField : additionalFields) {
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
