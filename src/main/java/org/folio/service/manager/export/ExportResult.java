package org.folio.service.manager.export;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import org.folio.util.ErrorCode;

@DataObject(generateConverter = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ExportResult {
  private ExportStatus status;
  private ErrorCode code;


  public ExportResult(JsonObject jsonObject) {
    ExportResultConverter.fromJson(jsonObject, this);
  }

  public ExportResult() {
  }

  public static ExportResult completed() {
    ExportResult exportResult = new ExportResult();
    exportResult.setStatus(ExportStatus.COMPLETED);
    return exportResult;
  }

  public static ExportResult inProgress() {
    ExportResult exportResult = new ExportResult();
    exportResult.setStatus(ExportStatus.IN_PROGRESS);
    return exportResult;
  }

  public static ExportResult error(ErrorCode errorCode) {
    ExportResult exportResult = new ExportResult();
    exportResult.setStatus(ExportStatus.ERROR);
    exportResult.setCode(errorCode);
    return exportResult;
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
  public boolean isError() {
    return this.status == ExportStatus.ERROR;
  }

  public JsonObject toJson() {
    return JsonObject.mapFrom(this);
  }

  public ExportStatus getStatus() {
    return status;
  }

  public ErrorCode getCode() {
    return code;
  }

  public void setStatus(ExportStatus status) {
    this.status = status;
  }

  public void setCode(ErrorCode code) {
    this.code = code;
  }

  public enum ExportStatus {
    COMPLETED,
    IN_PROGRESS,
    ERROR
  }

}
