package org.folio.service.mapping.reader;

import org.folio.service.mapping.processor.rule.Rule;
import org.folio.service.mapping.reader.values.MissingValue;
import org.folio.service.mapping.reader.values.RuleValue;
import org.folio.service.mapping.reader.values.SimpleValue;

public abstract class AbstractEntityReader implements EntityReader {

  @Override
  public RuleValue read(Rule rule) {
    if (isSimpleRule(rule)) {
      return readSimpleValue(rule);
    } else if (isCompositeRule(rule)) {
      return readCompositeValue(rule);
    }
    return MissingValue.getInstance();
  }

  private boolean isSimpleRule(Rule rule) {
    return rule.getDataSources().size() == 1;
  }

  private boolean isCompositeRule(Rule rule) {
    return rule.getDataSources().size() > 1;
  }

  protected abstract RuleValue readCompositeValue(Rule rule);

  protected abstract <S extends SimpleValue> S readSimpleValue(Rule rule);
}
