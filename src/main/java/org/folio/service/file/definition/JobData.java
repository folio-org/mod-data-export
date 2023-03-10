package org.folio.service.file.definition;

import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;


public class JobData {
  private final FileDefinition fileDefinition;
  private final JobExecution jobExecution;

  public JobData(FileDefinition fileDefinition, JobExecution jobExecution) {
    this.fileDefinition = fileDefinition;
    this.jobExecution = jobExecution;
  }

  public FileDefinition getFileDefinition() {
    return fileDefinition;
  }

  public JobExecution getJobExecution() {
    return jobExecution;
  }
}
