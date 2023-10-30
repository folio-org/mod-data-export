package org.folio.service.manager.export;

import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.util.OkapiConnectionParams;

import java.util.List;

/**
 * Payload of the export manager request.
 * Contains necessary information needed to export a chunk of data.
 */
public class ExportPayload {
  private List<String> identifiers;
  private boolean last;
  private FileDefinition fileExportDefinition;
  private OkapiConnectionParams okapiConnectionParams;
  private String jobExecutionId;
  private MappingProfile mappingProfile;
  private ExportRequest.RecordType recordType;
  private ExportRequest.IdType idType;
  private int exportedRecordsNumber;
  private int failedRecordsNumber;
  private int duplicatedSrs;
  private int invalidUUIDs;

  public ExportPayload() {
  }

  public ExportPayload(List<String> identifiers, boolean last, FileDefinition fileExportDefinition, OkapiConnectionParams okapiConnectionParams, String jobExecutionId, MappingProfile mappingProfile) {
    this.identifiers = identifiers;
    this.last = last;
    this.fileExportDefinition = fileExportDefinition;
    this.okapiConnectionParams = okapiConnectionParams;
    this.jobExecutionId = jobExecutionId;
    this.mappingProfile = mappingProfile;
  }

  public List<String> getIdentifiers() {
    return identifiers;
  }

  public void setIdentifiers(List<String> identifiers) {
    this.identifiers = identifiers;
  }

  public boolean isLast() {
    return last;
  }

  public void setLast(boolean last) {
    this.last = last;
  }

  public FileDefinition getFileExportDefinition() {
    return fileExportDefinition;
  }

  public void setFileExportDefinition(FileDefinition fileExportDefinition) {
    this.fileExportDefinition = fileExportDefinition;
  }

  public OkapiConnectionParams getOkapiConnectionParams() {
    return okapiConnectionParams;
  }

  public void setOkapiConnectionParams(OkapiConnectionParams okapiConnectionParams) {
    this.okapiConnectionParams = okapiConnectionParams;
  }

  public String getJobExecutionId() {
    return jobExecutionId;
  }

  public void setJobExecutionId(String jobExecutionId) {
    this.jobExecutionId = jobExecutionId;
  }

  public MappingProfile getMappingProfile() { return mappingProfile; }

  public void setMappingProfile(MappingProfile mappingProfile) { this.mappingProfile = mappingProfile; }

  public ExportRequest.RecordType getRecordType() { return recordType; }

  public void setRecordType(ExportRequest.RecordType recordType) { this.recordType = recordType; }

  public ExportRequest.IdType getIdType() {
    return idType;
  }

  public void setIdType(ExportRequest.IdType idType) {
    this.idType = idType;
  }

  public int getExportedRecordsNumber() {
    return exportedRecordsNumber;
  }

  public void setExportedRecordsNumber(int exportedRecordsNumber) {
    this.exportedRecordsNumber = exportedRecordsNumber;
  }

  public int getFailedRecordsNumber() {
    return failedRecordsNumber;
  }

  public void setFailedRecordsNumber(int failedRecordsNumber) {
    this.failedRecordsNumber = failedRecordsNumber;
  }

  public int getDuplicatedSrs() {
    return duplicatedSrs;
  }

  public void setDuplicatedSrs(int duplicatedSrs) {
    this.duplicatedSrs = duplicatedSrs;
  }

  public int getInvalidUUIDs() {
    return invalidUUIDs;
  }

  public void setInvalidUUIDs(int invalidUUIDs) {
    this.invalidUUIDs = invalidUUIDs;
  }
}
