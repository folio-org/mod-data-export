package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.TestUtil;
import org.folio.service.mapping.processor.translations.TranslationFunction;
import org.folio.service.mapping.processor.translations.TranslationsHolder;
import org.folio.service.mapping.settings.Settings;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class TranslationFunctionUnitTest {
  private static Settings settings = new Settings();

  @BeforeAll
  public static void setUp() {
    settings.addNatureOfContentTerms(getNatureOfContentTerms());
  }

  private static Map<String, JsonObject> getNatureOfContentTerms() {
    JsonObject natureOfContentTerm =
      new JsonObject(TestUtil.readFileContentFromResources("mockData/inventory/get_nature_of_content_terms_response.json"))
        .getJsonArray("natureOfContentTerms")
        .getJsonObject(0);
    return Collections.singletonMap(natureOfContentTerm.getString("id"), natureOfContentTerm);
  }

  @Test
  public void SetValue_shouldSetGivenValue() {
    // given
    TranslationFunction translationFunction = TranslationsHolder.lookup("set_value");
    String value = "field value";
    JsonObject parameter = new JsonObject().put("value", "new field value");
    // when
    String result = translationFunction.apply(value, parameter, null);
    // then
    Assert.assertEquals("new field value", result);
  }

  @Test
  public void SetNatureOfContentTerm_shouldReturnTermName() {
    // given
    TranslationFunction translationFunction = TranslationsHolder.lookup("set_nature_of_content_term");
    String value = "44cd89f3-2e76-469f-a955-cc57cb9e0395";
    // when
    String result = translationFunction.apply(value, null, settings);
    // then
    Assert.assertEquals("textbook", result);
  }

  @Test
  public void SetNatureOfContentTerm_shouldReturnEmptyString() {
    // given
    TranslationFunction translationFunction = TranslationsHolder.lookup("set_nature_of_content_term");
    String value = "non-existing-id";
    // when
    String result = translationFunction.apply(value, null, settings);
    // then
    Assert.assertEquals(StringUtils.EMPTY, result);
  }
}
