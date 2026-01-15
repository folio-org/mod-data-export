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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.UserInfo;
import org.hibernate.annotations.Type;

/** Entity representing a mapping profile. */
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "mapping_profiles")
public class MappingProfileEntity {

  /** Unique identifier of the mapping profile. */
  @Id private UUID id;

  /** Mapping profile details stored as JSONB. */
  @Type(JsonBinaryType.class)
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private MappingProfile mappingProfile;

  /** Date when the mapping profile was created. */
  private LocalDateTime creationDate;

  /** User ID who created the mapping profile. */
  private String createdBy;

  /** Name of the mapping profile. */
  private String name;

  /** Description of the mapping profile. */
  private String description;

  /** Record types associated with the mapping profile. */
  private String recordTypes;

  /** Output format of the mapping profile. */
  private String format;

  /** Date when the mapping profile was updated. */
  private LocalDateTime updatedDate;

  /** User ID who updated the mapping profile. */
  private String updatedByUserId;

  /** First name of the user who updated the mapping profile. */
  private String updatedByFirstName;

  /** Last name of the user who updated the mapping profile. */
  private String updatedByLastName;

  /** Indicates whether the mapping profile is locked. */
  @Column(nullable = false)
  private boolean locked;

  private UUID lockedBy;

  private LocalDateTime lockedAt;

  /** Creates a MappingProfileEntity from a MappingProfile DTO. */
  public static MappingProfileEntity fromMappingProfile(MappingProfile mappingProfile) {
    if (isNull(mappingProfile.getId())) {
      mappingProfile.setId(UUID.randomUUID());
    }
    var metadata = ofNullable(mappingProfile.getMetadata()).orElse(new Metadata());
    var userInfo = ofNullable(mappingProfile.getUserInfo()).orElse(new UserInfo());
    return MappingProfileEntity.builder()
        .id(mappingProfile.getId())
        .mappingProfile(mappingProfile)
        .creationDate(
            isNull(metadata.getCreatedDate())
                ? null
                : metadata.getCreatedDate().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime())
        .createdBy(metadata.getCreatedByUserId())
        .name(mappingProfile.getName())
        .description(mappingProfile.getDescription())
        .recordTypes(recordTypesToString(mappingProfile.getRecordTypes()))
        .format(
            isNull(mappingProfile.getOutputFormat())
                ? null
                : mappingProfile.getOutputFormat().getValue())
        .updatedDate(
            isNull(metadata.getUpdatedDate())
                ? null
                : metadata.getUpdatedDate().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime())
        .updatedByUserId(metadata.getUpdatedByUserId())
        .updatedByFirstName(userInfo.getFirstName())
        .updatedByLastName(userInfo.getLastName())
        .build();
  }

  /** Converts a list of RecordTypes to a comma-separated string. */
  private static String recordTypesToString(List<RecordTypes> recordTypes) {
    return isNull(recordTypes)
        ? null
        : recordTypes.stream().map(RecordTypes::getValue).collect(Collectors.joining(","));
  }
}
