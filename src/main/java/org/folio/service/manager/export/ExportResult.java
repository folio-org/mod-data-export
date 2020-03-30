package org.folio.service.manager.export;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import org.folio.util.ErrorCode;

@DataObject(generateConverter = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ExportResult {
  private static final ExportResult IN_PROGRESS = new ExportResult(ExportStatus.IN_PROGRESS);
  private static final ExportResult COMPLETED = new ExportResult(ExportStatus.COMPLETED);

  private ExportStatus exportStatus;
  private ErrorCode errorCode;

  private ExportResult(ExportStatus status) {
    this.exportStatus = status;
  }

  private ExportResult(ExportStatus status, ErrorCode errorCode) {
    this.exportStatus = status;
    this.errorCode = errorCode;
  }

  public ExportResult(JsonObject jsonObject) {
    ExportResultConverter.fromJson(jsonObject, this);
  }

  public static ExportResult completed() {
    return COMPLETED;
  }

  public static ExportResult inProgress() {
    return IN_PROGRESS;
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

  public void setStatus(ExportStatus status) {
    this.exportStatus = status;
  }

  public void setErrorCode(ErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  public enum ExportStatus {
    COMPLETED,
    IN_PROGRESS,
    FAILED
  }

}
