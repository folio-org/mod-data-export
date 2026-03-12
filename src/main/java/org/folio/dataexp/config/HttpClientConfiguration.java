package org.folio.dataexp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.client.AlternativeTitleTypesClient;
import org.folio.dataexp.client.AuthorityClient;
import org.folio.dataexp.client.CallNumberTypesClient;
import org.folio.dataexp.client.ConfigurationEntryClient;
import org.folio.dataexp.client.ConsortiaClient;
import org.folio.dataexp.client.ConsortiumClient;
import org.folio.dataexp.client.ConsortiumSearchClient;
import org.folio.dataexp.client.ContributorNameTypesClient;
import org.folio.dataexp.client.ElectronicAccessRelationshipsClient;
import org.folio.dataexp.client.EurekaUserPermissionsClient;
import org.folio.dataexp.client.HoldingsNoteTypesClient;
import org.folio.dataexp.client.IdentifierTypesClient;
import org.folio.dataexp.client.InstanceFormatsClient;
import org.folio.dataexp.client.InstanceTypesClient;
import org.folio.dataexp.client.IssuanceModesClient;
import org.folio.dataexp.client.ItemNoteTypesClient;
import org.folio.dataexp.client.LoanTypesClient;
import org.folio.dataexp.client.LocationUnitsClient;
import org.folio.dataexp.client.LocationsClient;
import org.folio.dataexp.client.MaterialTypesClient;
import org.folio.dataexp.client.NatureOfContentTermsClient;
import org.folio.dataexp.client.OkapiClient;
import org.folio.dataexp.client.OkapiUserPermissionsClient;
import org.folio.dataexp.client.QueryClient;
import org.folio.dataexp.client.SearchClient;
import org.folio.dataexp.client.SearchConsortiumHoldings;
import org.folio.dataexp.client.SourceStorageClient;
import org.folio.dataexp.client.UserClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Configuration class that defines HTTP client beans for various FOLIO module APIs.
 *
 * <p>Each bean is created via {@link HttpServiceProxyFactory} to produce a type-safe
 * HTTP service proxy for the corresponding client interface.
 */
@EnableRetry
@Configuration
@Log4j2
@RequiredArgsConstructor
public class HttpClientConfiguration {

  /**
   * Creates an {@link AlternativeTitleTypesClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link AlternativeTitleTypesClient}
   */
  @Bean
  public AlternativeTitleTypesClient addressTypeClient(HttpServiceProxyFactory factory) {
    return factory.createClient(AlternativeTitleTypesClient.class);
  }

  /**
   * Creates an {@link AuthorityClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link AuthorityClient}
   */
  @Bean
  public AuthorityClient authorityClient(HttpServiceProxyFactory factory) {
    return factory.createClient(AuthorityClient.class);
  }

  /**
   * Creates a {@link CallNumberTypesClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link CallNumberTypesClient}
   */
  @Bean
  public CallNumberTypesClient callNumberTypesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(CallNumberTypesClient.class);
  }

  /**
   * Creates a {@link ConfigurationEntryClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link ConfigurationEntryClient}
   */
  @Bean
  public ConfigurationEntryClient configurationEntryClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ConfigurationEntryClient.class);
  }

  /**
   * Creates a {@link ConsortiaClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link ConsortiaClient}
   */
  @Bean
  public ConsortiaClient consortiaClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ConsortiaClient.class);
  }

  /**
   * Creates a {@link ConsortiumClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link ConsortiumClient}
   */
  @Bean
  public ConsortiumClient consortiumClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ConsortiumClient.class);
  }

  /**
   * Creates a {@link ConsortiumSearchClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link ConsortiumSearchClient}
   */
  @Bean
  public ConsortiumSearchClient consortiumSearchClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ConsortiumSearchClient.class);
  }

  /**
   * Creates a {@link ContributorNameTypesClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link ContributorNameTypesClient}
   */
  @Bean
  public ContributorNameTypesClient contributorNameTypesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ContributorNameTypesClient.class);
  }

  /**
   * Creates an {@link ElectronicAccessRelationshipsClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link ElectronicAccessRelationshipsClient}
   */
  @Bean
  public ElectronicAccessRelationshipsClient electronicAccessRelationshipsClient(
      HttpServiceProxyFactory factory) {
    return factory.createClient(ElectronicAccessRelationshipsClient.class);
  }

  /**
   * Creates a {@link EurekaUserPermissionsClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link EurekaUserPermissionsClient}
   */
  @Bean
  public EurekaUserPermissionsClient eurekaUserPermissionsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(EurekaUserPermissionsClient.class);
  }

  /**
   * Creates a {@link HoldingsNoteTypesClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link HoldingsNoteTypesClient}
   */
  @Bean
  public HoldingsNoteTypesClient holdingsNoteTypesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(HoldingsNoteTypesClient.class);
  }

  /**
   * Creates an {@link IdentifierTypesClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link IdentifierTypesClient}
   */
  @Bean
  public IdentifierTypesClient identifierTypesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(IdentifierTypesClient.class);
  }

  /**
   * Creates an {@link InstanceFormatsClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link InstanceFormatsClient}
   */
  @Bean
  public InstanceFormatsClient instanceFormatsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(InstanceFormatsClient.class);
  }

  /**
   * Creates an {@link InstanceTypesClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link InstanceTypesClient}
   */
  @Bean
  public InstanceTypesClient instanceTypesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(InstanceTypesClient.class);
  }

  /**
   * Creates an {@link IssuanceModesClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link IssuanceModesClient}
   */
  @Bean
  public IssuanceModesClient issuanceModesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(IssuanceModesClient.class);
  }

  /**
   * Creates an {@link ItemNoteTypesClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link ItemNoteTypesClient}
   */
  @Bean
  public ItemNoteTypesClient itemNoteTypesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ItemNoteTypesClient.class);
  }

  /**
   * Creates a {@link LoanTypesClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link LoanTypesClient}
   */
  @Bean
  public LoanTypesClient loanTypesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LoanTypesClient.class);
  }

  /**
   * Creates a {@link LocationsClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link LocationsClient}
   */
  @Bean
  public LocationsClient locationsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LocationsClient.class);
  }

  /**
   * Creates a {@link LocationUnitsClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link LocationUnitsClient}
   */
  @Bean
  public LocationUnitsClient locationUnitsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(LocationUnitsClient.class);
  }

  /**
   * Creates a {@link MaterialTypesClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link MaterialTypesClient}
   */
  @Bean
  public MaterialTypesClient materialTypesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(MaterialTypesClient.class);
  }

  /**
   * Creates a {@link NatureOfContentTermsClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link NatureOfContentTermsClient}
   */
  @Bean
  public NatureOfContentTermsClient natureOfContentTermsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(NatureOfContentTermsClient.class);
  }

  /**
   * Creates an {@link OkapiClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link OkapiClient}
   */
  @Bean
  public OkapiClient okapiClient(HttpServiceProxyFactory factory) {
    return factory.createClient(OkapiClient.class);
  }

  /**
   * Creates an {@link OkapiUserPermissionsClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link OkapiUserPermissionsClient}
   */
  @Bean
  public OkapiUserPermissionsClient okapiUserPermissionsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(OkapiUserPermissionsClient.class);
  }

  /**
   * Creates a {@link QueryClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link QueryClient}
   */
  @Bean
  public QueryClient queryClient(HttpServiceProxyFactory factory) {
    return factory.createClient(QueryClient.class);
  }

  /**
   * Creates a {@link SearchClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link SearchClient}
   */
  @Bean
  public SearchClient searchClient(HttpServiceProxyFactory factory) {
    return factory.createClient(SearchClient.class);
  }

  /**
   * Creates a {@link SearchConsortiumHoldings} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link SearchConsortiumHoldings}
   */
  @Bean
  public SearchConsortiumHoldings searchConsortiumHoldings(HttpServiceProxyFactory factory) {
    return factory.createClient(SearchConsortiumHoldings.class);
  }

  /**
   * Creates a {@link SourceStorageClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link SourceStorageClient}
   */
  @Bean
  public SourceStorageClient sourceStorageClient(HttpServiceProxyFactory factory) {
    return factory.createClient(SourceStorageClient.class);
  }

  /**
   * Creates a {@link UserClient} bean.
   *
   * @param factory the {@link HttpServiceProxyFactory} used to create the client proxy
   * @return a proxy instance of {@link UserClient}
   */
  @Bean
  public UserClient userClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UserClient.class);
  }

}
