package org.folio.service.manager.inputdatamanager;

import com.google.common.collect.Lists;
import io.vertx.core.Context;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.rest.impl.HttpServerTestBase;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.manager.exportmanager.ExportManager;
import org.folio.service.manager.inputdatamanager.reader.SourceReader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InputDataManagerTest extends HttpServerTestBase {

  private static final int BATCH_SIZE = 5;
  private static final String fileName = "InventoryUUIDs.csv";

  private InputDataManager inputDataManager;
  private ExportManager exportManager;
  private JsonObject exportRequest;
  @Mock
  private SourceReader sourceReader;

  @Captor
  ArgumentCaptor<JsonObject> exportRequestWithIdentifiers;
  private String[] expectedIdentifiers;

  private static List<String> expectedIds =
    Arrays.asList("c8b50e3f-0446-429c-960e-03774b88223f",
      "aae06d90-a8c2-4514-b227-5756f1f5f5d6",
      "d5c7968c-17e7-4ab1-8aeb-3109e1b77c80",
      "a5e9ccb3-737b-43b0-8f4a-f32a04c9ae16",
      "c5d662af-b0be-4851-bb9c-de70bba3dfce");


  private JsonObject requestParams;


  @Before
  public void setUp() {
    Context ctx = vertx.getOrCreateContext();
    inputDataManager = new InputDataManagerImpl(ctx);
    when(sourceReader.getSourceStream(any(FileDefinition.class), anyInt())).thenReturn(mockIterator());
    exportManager = ExportManager.create(vertx);
    // TODO create export manager
    // TODO create source reader
    ctx.put(SourceReader.class.getName(), sourceReader);
    ctx.put(ExportManager.class.getName(), exportManager);

    FileDefinition fileDefinition = new FileDefinition().withFileName(fileName).withSourcePath("src/test/resources/" + fileName);
    exportRequest = JsonObject.mapFrom(new ExportRequest().withFileDefinition(fileDefinition));
    requestParams = JsonObject.mapFrom(getOkapiConnectionParams());

    inputDataManager.init(exportRequest, requestParams);
  }

  @Test
  public void testInputFileIsFullProcessed() {

    inputDataManager.proceed(exportRequest, requestParams);
    //TODO
    //verify(exportManager, times(1)).export(exportRequestWithIdentifiers.capture(), any(OkapiConnectionParams.class));
    JsonObject entries = exportRequestWithIdentifiers.getValue();
    List actualIdentifiers = entries.getJsonArray("identifiers").getList();
    assertThat(actualIdentifiers, equalTo(expectedIds));
  }

  private Iterator<List<String>> mockIterator() {
    return Lists.partition(expectedIds, BATCH_SIZE).iterator();
  }
}
