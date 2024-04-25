package org.folio.dataexp.service.export.strategies;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.dataexp.domain.dto.MappingProfile;
import org.junit.jupiter.api.Test;
import org.marc4j.marc.impl.DataFieldImpl;
import org.marc4j.marc.impl.LeaderImpl;
import org.marc4j.marc.impl.RecordImpl;

class MarcSuppressProcessorTest {
  @Test
  void shouldSuppressListedFields() {
    var mappingProfile = MappingProfile.builder().fieldsSuppression("500,600").build();
    var record = new RecordImpl();
    record.setLeader(new LeaderImpl("01428nam a22003733c 4500"));
    record.addVariableField(new DataFieldImpl("400", 'f', 'f'));
    record.addVariableField(new DataFieldImpl("500", 'f', 'f'));
    record.addVariableField(new DataFieldImpl("600", 'f', 'f'));
    var processor = new MarcSuppressProcessor(mappingProfile);
    processor.suppress(record);
    assertThat(record.getVariableFields()).hasSize(1);
    assertThat(record.getVariableField("400")).isNotNull();
    assertThat(record.getVariableField("500")).isNull();
    assertThat(record.getVariableField("600")).isNull();
  }

  @Test
  void shouldSuppress999ff() {
    var mappingProfile = MappingProfile.builder().suppress999ff(true).build();
    var record = new RecordImpl();
    record.setLeader(new LeaderImpl("01428nam a22003733c 4500"));
    record.addVariableField(new DataFieldImpl("999", 'f', 'f'));
    record.addVariableField(new DataFieldImpl("999", 'f', ' '));
    var processor = new MarcSuppressProcessor(mappingProfile);
    processor.suppress(record);
    assertThat(record.getDataFields()).hasSize(1);
    var dataField = record.getDataFields().get(0);
    assertThat(dataField.getIndicator1()).isEqualTo('f');
    assertThat(dataField.getIndicator2()).isEqualTo(' ');
  }
}
