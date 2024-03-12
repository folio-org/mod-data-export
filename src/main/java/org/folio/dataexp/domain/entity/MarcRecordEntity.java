package org.folio.dataexp.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.UUID;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "v_marc_records_lb")
public class MarcRecordEntity {

  @Id
  private UUID id;
  private UUID externalId;
  @Column(name = "content", columnDefinition = "jsonb")
  private String content;
  private String recordType;
  private String state;
  private Character leaderRecordStatus;
  private Boolean suppressDiscovery;
}
