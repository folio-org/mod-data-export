package org.folio.dataexp.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.With;

import java.util.UUID;

@Getter
@Builder
@Entity
@Table(name = "v_holdings_record")
public class HoldingsRecordEntity {

  @Id
  private UUID id;
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private String jsonb;
  private UUID instanceId;
}
