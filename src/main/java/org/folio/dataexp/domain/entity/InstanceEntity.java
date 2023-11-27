package org.folio.dataexp.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity
@Table(name = "v_instance")
public class InstanceEntity {

  @Id
  private UUID id;
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private String jsonb;
}