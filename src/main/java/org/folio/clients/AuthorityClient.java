package org.folio.clients;

import org.springframework.stereotype.Component;

@Component
public class AuthorityClient extends BaseConcurrentClient {

  public static final String AUTHORITIES = "authorities";

  @Override
  public String getEntitiesCollectionName() {
    return AUTHORITIES;
  }
}
