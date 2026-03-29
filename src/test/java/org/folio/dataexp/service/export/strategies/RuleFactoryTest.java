package org.folio.dataexp.service.export.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.processor.rule.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.ArrayList;
import java.util.Optional;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.exception.TransformationRuleException;

@ExtendWith(MockitoExtension.class)
class RuleFactoryTest {

    @Mock
private List<Rule> defaultRulesFromConfigFile;

    @Mock
private List<Rule> defaultHoldingsRulesFromConfigFile;

    @InjectMocks
private RuleFactory ruleFactory;

    @Test
void shouldReturnEmpty_whenRecordTypeIsNotInstance() throws TransformationRuleException {
  // TestMate-d999471200a03a6a5b0d63efe04dbe53
  // Given
  Transformations transformations = new Transformations();
  transformations.setEnabled(true);
  transformations.setFieldId("holdings.hrid");
  transformations.setRecordType(RecordTypes.HOLDINGS);
  List<Rule> defaultRules = new ArrayList<>();
  // When
  Optional<Rule> result = ruleFactory.createDefaultByTransformations(transformations, defaultRules);
  // Then
  assertThat(result).isEmpty();
}

}
