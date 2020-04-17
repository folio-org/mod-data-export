package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;
import org.folio.service.mapping.processor.translations.Settings;
import org.folio.service.mapping.processor.translations.TranslationFunction;
import org.folio.service.mapping.processor.translations.TranslationsHolder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TranslationFunctionUnitTest {
  private Settings settings;

  @Test
  public void shouldSetValue() {
    // given
    TranslationFunction translationFunction = TranslationsHolder.lookup("set_value");
    String value = "field value";
    JsonObject parameter = new JsonObject().put("value", "new field value");
    // when
    String result = translationFunction.apply(value, parameter, settings);
    // then
    Assert.assertEquals("new field value", result);
  }
}
