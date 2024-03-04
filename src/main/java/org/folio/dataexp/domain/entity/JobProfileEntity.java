package org.folio.dataexp.domain.entity;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.UserInfo;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "job_profiles")
public class JobProfileEntity {

  @Id
  private UUID id;

  @Type(JsonBinaryType.class)
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private JobProfile jobProfile;

  private LocalDateTime creationDate;

  private String createdBy;

  private String name;
  private String description;
  private LocalDateTime updatedDate;
  private String updatedByUserId;
  private String updatedByFirstName;
  private String updatedByLastName;

  @Column(name = "mappingprofileid")
  private UUID mappingProfileId;

  public static JobProfileEntity fromJobProfile(JobProfile jobProfile) {
    if (isNull(jobProfile.getId())) {
      jobProfile.setId(UUID.randomUUID());
    }
    var metadata = ofNullable(jobProfile.getMetadata()).orElse(new Metadata());
    var userInfo = ofNullable(jobProfile.getUserInfo()).orElse(new UserInfo());
    return JobProfileEntity.builder()
      .id(jobProfile.getId())
      .jobProfile(jobProfile)
      .creationDate(isNull(metadata.getCreatedDate()) ? null : metadata.getCreatedDate().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime())
      .createdBy(metadata.getCreatedByUserId())
      .name(jobProfile.getName())
      .description(jobProfile.getDescription())
      .updatedDate(isNull(metadata.getUpdatedDate()) ? null : metadata.getUpdatedDate().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime())
      .updatedByUserId(metadata.getUpdatedByUserId())
      .updatedByFirstName(userInfo.getFirstName())
      .updatedByLastName(userInfo.getLastName())
      .mappingProfileId(jobProfile.getMappingProfileId())
      .build();
  }
}
