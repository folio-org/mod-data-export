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
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.UserInfo;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "mapping_profiles")
public class MappingProfileEntity {

  @Id
  private UUID id;

  @Type(JsonBinaryType.class)
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private MappingProfile mappingProfile;

  private LocalDateTime creationDate;

  private String createdBy;

  private String name;
  private String description;
  private String recordTypes;
  private String format;
  private LocalDateTime updatedDate;
  private String updatedByUserId;
  private String updatedByFirstName;
  private String updatedByLastName;

  public static MappingProfileEntity fromMappingProfile(MappingProfile mappingProfile) {
    if (isNull(mappingProfile.getId())) {
      mappingProfile.setId(UUID.randomUUID());
    }
    var metadata = ofNullable(mappingProfile.getMetadata()).orElse(new Metadata());
    var userInfo = ofNullable(mappingProfile.getUserInfo()).orElse(new UserInfo());
    return MappingProfileEntity.builder()
      .id(mappingProfile.getId())
      .mappingProfile(mappingProfile)
      .creationDate(isNull(metadata.getCreatedDate()) ? null : metadata.getCreatedDate().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime())
      .createdBy(metadata.getCreatedByUserId())
      .name(mappingProfile.getName())
      .description(mappingProfile.getDescription())
      .recordTypes(recordTypesToString(mappingProfile.getRecordTypes()))
      .format(isNull(mappingProfile.getOutputFormat()) ? null : mappingProfile.getOutputFormat().getValue())
      .updatedDate(isNull(metadata.getUpdatedDate()) ? null : metadata.getUpdatedDate().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime())
      .updatedByUserId(metadata.getUpdatedByUserId())
      .updatedByFirstName(userInfo.getFirstName())
      .updatedByLastName(userInfo.getLastName())
      .build();
  }

  private static String recordTypesToString(List<RecordTypes> recordTypes) {
    return isNull(recordTypes) ?
      null :
      recordTypes.stream()
        .map(RecordTypes::getValue)
        .collect(Collectors.joining(","));
  }
}
