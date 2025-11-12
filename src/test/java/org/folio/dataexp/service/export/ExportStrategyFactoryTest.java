package org.folio.dataexp.service.export;

import static org.folio.dataexp.service.export.Constants.DEFAULT_AUTHORITY_MAPPING_PROFILE_ID;
import static org.folio.dataexp.service.export.Constants.DEFAULT_HOLDINGS_MAPPING_PROFILE_ID;
import static org.folio.dataexp.service.export.Constants.DEFAULT_INSTANCE_MAPPING_PROFILE_ID;
import static org.folio.dataexp.service.export.Constants.DEFAULT_LINKED_DATA_MAPPING_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_AUTHORITY_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_HOLDINGS_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_INSTANCE_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_LINKED_DATA_JOB_PROFILE_ID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.stream.Stream;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.service.export.strategies.AuthorityExportAllStrategy;
import org.folio.dataexp.service.export.strategies.AuthorityExportStrategy;
import org.folio.dataexp.service.export.strategies.HoldingsExportAllStrategy;
import org.folio.dataexp.service.export.strategies.HoldingsExportStrategy;
import org.folio.dataexp.service.export.strategies.InstancesExportAllStrategy;
import org.folio.dataexp.service.export.strategies.InstancesExportStrategy;
import org.folio.dataexp.service.export.strategies.ld.LinkedDataExportStrategy;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportStrategyFactoryTest {

  @Mock
  private HoldingsExportStrategy holdingsExportStrategy;
  @Mock
  private InstancesExportStrategy instancesExportStrategy;
  @Mock
  private AuthorityExportStrategy authorityExportStrategy;
  @Mock
  private HoldingsExportAllStrategy holdingsExportAllStrategy;
  @Mock
  private InstancesExportAllStrategy instancesExportAllStrategy;
  @Mock
  private AuthorityExportAllStrategy authorityExportAllStrategy;
  @Mock
  private LinkedDataExportStrategy linkedDataExportStrategy;
  @Mock
  private JobProfileEntityRepository jobProfileEntityRepository;
  @Mock
  private MappingProfileEntityRepository mappingProfileEntityRepository;

  @InjectMocks
  private ExportStrategyFactory exportStrategyFactory;

  static Stream<Arguments> exportStrategyArguments() {
    return Stream.of(
      Arguments.of(
        UUID.fromString(DEFAULT_INSTANCE_JOB_PROFILE_ID),
        UUID.fromString(DEFAULT_INSTANCE_MAPPING_PROFILE_ID),
        RecordTypes.INSTANCE,
        null,
        false,
        InstancesExportStrategy.class
      ),
      Arguments.of(
        UUID.fromString(DEFAULT_INSTANCE_JOB_PROFILE_ID),
        UUID.fromString(DEFAULT_INSTANCE_MAPPING_PROFILE_ID),
        RecordTypes.INSTANCE,
        null,
        true,
        InstancesExportAllStrategy.class
      ),
      Arguments.of(
        UUID.fromString(DEFAULT_HOLDINGS_JOB_PROFILE_ID),
        UUID.fromString(DEFAULT_HOLDINGS_MAPPING_PROFILE_ID),
        RecordTypes.HOLDINGS,
        null,
        false,
        HoldingsExportStrategy.class
      ),
      Arguments.of(
        UUID.fromString(DEFAULT_HOLDINGS_JOB_PROFILE_ID),
        UUID.fromString(DEFAULT_HOLDINGS_MAPPING_PROFILE_ID),
        RecordTypes.HOLDINGS,
        null,
        true,
        HoldingsExportAllStrategy.class
      ),
      Arguments.of(
        UUID.fromString(DEFAULT_AUTHORITY_JOB_PROFILE_ID),
        UUID.fromString(DEFAULT_AUTHORITY_MAPPING_PROFILE_ID),
        RecordTypes.AUTHORITY,
        null,
        false,
        AuthorityExportStrategy.class
      ),
      Arguments.of(
        UUID.fromString(DEFAULT_AUTHORITY_JOB_PROFILE_ID),
        UUID.fromString(DEFAULT_AUTHORITY_MAPPING_PROFILE_ID),
        RecordTypes.AUTHORITY,
        null,
        true,
        AuthorityExportAllStrategy.class
      ),
      Arguments.of(
        UUID.fromString(DEFAULT_LINKED_DATA_JOB_PROFILE_ID),
        UUID.fromString(DEFAULT_LINKED_DATA_MAPPING_PROFILE_ID),
        RecordTypes.LINKED_DATA,
        null,
        false,
        LinkedDataExportStrategy.class
      ),
      Arguments.of(
        UUID.randomUUID(),
        UUID.randomUUID(),
        RecordTypes.INSTANCE,
        ExportRequest.IdTypeEnum.INSTANCE,
        false,
        InstancesExportStrategy.class
      ),
      Arguments.of(
        UUID.randomUUID(),
        UUID.randomUUID(),
        RecordTypes.INSTANCE,
        ExportRequest.IdTypeEnum.INSTANCE,
        true,
        InstancesExportAllStrategy.class
      ),
      Arguments.of(
        UUID.randomUUID(),
        UUID.randomUUID(),
        RecordTypes.HOLDINGS,
        ExportRequest.IdTypeEnum.HOLDING,
        false,
        HoldingsExportStrategy.class
      ),
      Arguments.of(
        UUID.randomUUID(),
        UUID.randomUUID(),
        RecordTypes.HOLDINGS,
        ExportRequest.IdTypeEnum.HOLDING,
        true,
        HoldingsExportAllStrategy.class
      ),
      Arguments.of(
        UUID.randomUUID(),
        UUID.randomUUID(),
        RecordTypes.AUTHORITY,
        ExportRequest.IdTypeEnum.AUTHORITY,
        false,
        AuthorityExportStrategy.class
      ),
      Arguments.of(
        UUID.randomUUID(),
        UUID.randomUUID(),
        RecordTypes.AUTHORITY,
        ExportRequest.IdTypeEnum.AUTHORITY,
        true,
        AuthorityExportAllStrategy.class
      )
    );
  }

  @ParameterizedTest
  @MethodSource("exportStrategyArguments")
  void getExportStrategyTest(
      UUID jobProfileId,
      UUID mappingProfileId,
      RecordTypes recordType,
      ExportRequest.IdTypeEnum idType,
      boolean exportAll,
      Class<?> strategyClass
  ) {
    var jobProfile = new JobProfile()
        .id(jobProfileId)
        .mappingProfileId(mappingProfileId);
    var jobProfileEntity = new JobProfileEntity()
        .withId(jobProfileId)
        .withMappingProfileId(mappingProfileId)
        .withJobProfile(jobProfile);
    when(jobProfileEntityRepository.getReferenceById(jobProfileId))
        .thenReturn(jobProfileEntity);
    var mappingProfile = new MappingProfile()
        .id(mappingProfileId)
        .addRecordTypesItem(recordType);
    var mappingProfileEntity = new MappingProfileEntity()
        .withId(mappingProfileId)
        .withMappingProfile(mappingProfile)
        .withRecordTypes(recordType.toString());
    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    var exportRequest = new ExportRequest()
        .jobProfileId(jobProfileId);
    if (idType != null) {
      exportRequest.setIdType(idType);
    }
    if (exportAll) {
      exportRequest.setAll(true);
    }

    var strategy = exportStrategyFactory.getExportStrategy(exportRequest);

    assertTrue(strategyClass.isInstance(strategy));
  }
}
