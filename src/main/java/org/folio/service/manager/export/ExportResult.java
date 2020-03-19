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

  private ExportStatus status;
  private ErrorCode errorCode;

  private ExportResult(ExportStatus status) {
    this.status = status;
  }

  private ExportResult(ExportStatus status, ErrorCode errorCode) {
    this.status = status;
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
    return this.status == ExportStatus.COMPLETED;
  }

  @JsonIgnore
  public boolean isInProgress() {
    return this.status == ExportStatus.IN_PROGRESS;
  }

  @JsonIgnore
  public boolean isFailed() {
    return this.status == ExportStatus.FAILED;
  }

  public JsonObject toJson() {
    return JsonObject.mapFrom(this);
  }

  public ExportStatus getStatus() {
    return status;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public void setStatus(ExportStatus status) {
    this.status = status;
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
