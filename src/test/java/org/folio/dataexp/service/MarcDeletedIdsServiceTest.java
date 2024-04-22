package org.folio.dataexp.service;

import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.client.ConsortiaClient;
import org.folio.dataexp.client.SourceStorageClient;
import org.folio.dataexp.domain.dto.*;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class MarcDeletedIdsServiceTest extends BaseDataExportInitializer {

  private final static String LEADER_SEARCH_EXPRESSION_DELETED = "p_05 = 'd'";

  @Autowired
  private SourceStorageClient sourceStorageClient;

  @MockBean
  private ConsortiaClient consortiaClient;

  @Test
  void shouldReturnOneLocalRecord() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var payload = new MarcRecordIdentifiersPayload()
        .withLeaderSearchExpression(LEADER_SEARCH_EXPRESSION_DELETED);
      var res = sourceStorageClient.getMarcRecordsIdentifiers(payload, "central");
    }
  }
}
