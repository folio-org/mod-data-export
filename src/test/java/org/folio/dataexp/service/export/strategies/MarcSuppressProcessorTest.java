package org.folio.dataexp.service.export.strategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    var record1 = new RecordImpl();
    record1.setLeader(new LeaderImpl("01428nam a22003733c 4500"));
    record1.addVariableField(new DataFieldImpl("999", 'f', 'f'));
    var processor = new MarcSuppressProcessor(mappingProfile);
    processor.suppress(record1);
    assertNull(record1.getVariableField("999"));

    var record2 = new RecordImpl();
    record2.setLeader(new LeaderImpl("01428nam a22003733c 4500"));
    record2.addVariableField(new DataFieldImpl("999", 'f', ' '));
    processor = new MarcSuppressProcessor(mappingProfile);
    processor.suppress(record2);
    assertNotNull(record2.getVariableField("999"));
  }
}
