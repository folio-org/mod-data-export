package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;
import org.folio.service.mapping.settings.Settings;
import org.folio.service.mapping.processor.translations.TranslationFunction;
import org.folio.service.mapping.processor.translations.TranslationsHolder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class TranslationFunctionUnitTest {

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
    String value = "96879b60-098b-453b-bf9a-c47866f1ab2a";
    Settings settings = new Settings();
    JsonObject natureOfContentTerm_audioBook = new JsonObject()
      .put("id", "96879b60-098b-453b-bf9a-c47866f1ab2a")
      .put("name", "audiobook")
      .put("source", "folio");
    settings.addNatureOfContentTerms(Collections.singletonList(natureOfContentTerm_audioBook));
    // when
    String result = translationFunction.apply(value, null, settings);
    // then
    Assert.assertEquals("audiobook", result);
  }

  @Test
  public void SetNatureOfContentTerm_shouldReturnNull() {
    // given
    TranslationFunction translationFunction = TranslationsHolder.lookup("set_nature_of_content_term");
    String value = "non-existing-id";
    Settings settings = new Settings();
    JsonObject natureOfContentTerm_audioBook = new JsonObject()
      .put("id", "96879b60-098b-453b-bf9a-c47866f1ab2a")
      .put("name", "audiobook")
      .put("source", "folio");
    settings.addNatureOfContentTerms(Collections.singletonList(natureOfContentTerm_audioBook));
    // when
    String result = translationFunction.apply(value, null, settings);
    // then
    Assert.assertNull(result);
  }
}
