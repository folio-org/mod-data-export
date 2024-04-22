package org.folio.dataexp.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class FeignInterceptor implements RequestInterceptor {
  @Override
  public void apply(RequestTemplate requestTemplate) {
    log.info("requesttemplate: {}, tenant: {}", requestTemplate, requestTemplate.headers());
  }
}
