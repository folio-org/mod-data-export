package org.folio.dataexp.domain.entity;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.UserInfo;
import org.hibernate.annotations.Type;

/**
 * Entity representing a job profile.
 */
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "job_profiles")
public class JobProfileEntity {

  /**
   * Unique identifier of the job profile.
   */
  @Id
  private UUID id;

  /**
   * Job profile details stored as JSONB.
   */
  @Type(JsonBinaryType.class)
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private JobProfile jobProfile;

  /**
   * Date when the job profile was created.
   */
  private LocalDateTime creationDate;

  /**
   * User ID who created the job profile.
   */
  private String createdBy;

  /**
   * Name of the job profile.
   */
  private String name;

  /**
   * Description of the job profile.
   */
  private String description;

  /**
   * Date when the job profile was updated.
   */
  private LocalDateTime updatedDate;

  /**
   * User ID who updated the job profile.
   */
  private String updatedByUserId;

  /**
   * First name of the user who updated the job profile.
   */
  private String updatedByFirstName;

  /**
   * Last name of the user who updated the job profile.
   */
  private String updatedByLastName;

  /**
   * Mapping profile ID associated with the job profile.
   */
  @Column(name = "mappingprofileid")
  private UUID mappingProfileId;

  /**
   * Creates a JobProfileEntity from a JobProfile DTO.
   */
  public static JobProfileEntity fromJobProfile(JobProfile jobProfile) {
    if (isNull(jobProfile.getId())) {
      jobProfile.setId(UUID.randomUUID());
    }
    var metadata = ofNullable(jobProfile.getMetadata()).orElse(new Metadata());
    var userInfo = ofNullable(jobProfile.getUserInfo()).orElse(new UserInfo());
    return JobProfileEntity.builder()
      .id(jobProfile.getId())
      .jobProfile(jobProfile)
      .creationDate(isNull(metadata.getCreatedDate()) ? null : metadata.getCreatedDate()
        .toInstant().atZone(ZoneOffset.UTC).toLocalDateTime())
      .createdBy(metadata.getCreatedByUserId())
      .name(jobProfile.getName())
      .description(jobProfile.getDescription())
      .updatedDate(isNull(metadata.getUpdatedDate()) ? null : metadata.getUpdatedDate()
        .toInstant().atZone(ZoneOffset.UTC).toLocalDateTime())
      .updatedByUserId(metadata.getUpdatedByUserId())
      .updatedByFirstName(userInfo.getFirstName())
      .updatedByLastName(userInfo.getLastName())
      .mappingProfileId(jobProfile.getMappingProfileId())
      .build();
  }
}
