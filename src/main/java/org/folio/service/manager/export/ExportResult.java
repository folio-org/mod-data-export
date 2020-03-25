package org.folio.service.manager.export;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import org.folio.util.ErrorCode;

@DataObject(generateConverter = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ExportResult {

  private ExportStatus exportStatus;
  private ErrorCode errorCode;
  private int recordsNumber;

  private ExportResult(ExportStatus status, int recordsNumber) {
    this.exportStatus = status;
    this.recordsNumber = recordsNumber;
  }

  private ExportResult(ExportStatus status, ErrorCode errorCode) {
    this.exportStatus = status;
    this.errorCode = errorCode;
  }

  public ExportResult(JsonObject jsonObject) {
    ExportResultConverter.fromJson(jsonObject, this);
  }

  public static ExportResult completed(int recordsNumber) {
    return new ExportResult(ExportStatus.COMPLETED, recordsNumber);
  }

  public static ExportResult inProgress(int recordsNumber) {
    return new ExportResult(ExportStatus.IN_PROGRESS, recordsNumber);
  }

  public static ExportResult failed(ErrorCode errorCode) {
    return new ExportResult(ExportStatus.FAILED, errorCode);
  }

  @JsonIgnore
  public boolean isCompleted() {
    return this.exportStatus == ExportStatus.COMPLETED;
  }

  @JsonIgnore
  public boolean isInProgress() {
    return this.exportStatus == ExportStatus.IN_PROGRESS;
  }

  @JsonIgnore
  public boolean isFailed() {
    return this.exportStatus == ExportStatus.FAILED;
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    ExportResultConverter.toJson(this, jsonObject);
    return jsonObject;
  }

  public ExportStatus getStatus() {
    return exportStatus;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public int getRecordsNumber() {
    return recordsNumber;
  }

  public void setStatus(ExportStatus status) {
    this.exportStatus = status;
  }

  public void setErrorCode(ErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  public void setRecordsNumber(int recordsNumber) {
    this.recordsNumber = recordsNumber;
  }

  public enum ExportStatus {
    COMPLETED,
    IN_PROGRESS,
    FAILED
  }

}
