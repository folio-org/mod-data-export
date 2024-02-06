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
@Table(name = "v_instance_all_deleted")
public class InstanceDeletedEntity {

  @Id
  private UUID id;
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private String jsonb;
}
