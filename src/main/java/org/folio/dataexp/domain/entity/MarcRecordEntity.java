package org.folio.dataexp.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity
@Table(name = "v_marc_records_lb")
public class MarcRecordEntity {

  @Id
  private UUID id;
  private UUID externalId;
  @Column(name = "content", columnDefinition = "jsonb")
  private String content;
}
