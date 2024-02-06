package org.folio.dataexp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.github.jknack.handlebars.internal.Files;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = BaseDataExportInitializer.Initializer.class)
@Testcontainers
@AutoConfigureMockMvc
@Log4j2
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class BaseDataExportInitializer {

  protected static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6IjFkM2I1OGNiLTA3YjUtNWZjZC04YTJhLTNjZTA2YTBlYjkwZiIsImlhdCI6MTYxNjQyMDM5MywidGVuYW50IjoiZGlrdSJ9.2nvEYQBbJP1PewEgxixBWLHSX_eELiBEBpjufWiJZRs";
  protected static final String TENANT = "diku";
  protected static final UUID DEFAULT_HOLDINGS_JOB_PROFILE = UUID.fromString("5e9835fc-0e51-44c8-8a47-f7b8fce35da7");
  protected static final UUID DEFAULT_AUTHORITY_JOB_PROFILE = UUID.fromString("56944b1c-f3f9-475b-bed0-7387c33620ce");
  public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
  public static final String S3_ACCESS_KEY = "minio-access-key";
  public static final String S3_SECRET_KEY = "minio-secret-key";
  public static final int S3_PORT = 9000;
  public static final String BUCKET = "test-bucket";
  public static final String REGION = "us-west-2";
  private static final String MINIO_ENDPOINT;

  public static final PostgreSQLContainer<?> postgresDBContainer;
  private static final GenericContainer<?> s3;

  static {
    postgresDBContainer = new PostgreSQLContainer<>("postgres:12");
    postgresDBContainer.start();
    s3 = new GenericContainer<>("minio/minio:latest")
      .withEnv("MINIO_ACCESS_KEY", S3_ACCESS_KEY)
      .withEnv("MINIO_SECRET_KEY", S3_SECRET_KEY)
      .withCommand("server /data")
      .withExposedPorts(S3_PORT)
      .waitingFor(new HttpWaitStrategy().forPath("/minio/health/ready")
        .forPort(S3_PORT)
        .withStartupTimeout(Duration.ofSeconds(10))
      );
    s3.start();
    MINIO_ENDPOINT = format("http://%s:%s", s3.getHost(), s3.getFirstMappedPort());

    try {
      createDataAndTablesForViews();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void createDataAndTablesForViews() throws IOException{
    var dataSource =  new SingleConnectionDataSource(postgresDBContainer.getJdbcUrl(),postgresDBContainer.getUsername(), postgresDBContainer.getPassword(), true );
    var jdbcTemplate = new JdbcTemplate(dataSource);
    runSqlScript("/init_public_schema.sql", jdbcTemplate);
    runSqlScript("/init_mod_inventory_storage.sql", jdbcTemplate);
    runSqlScript("/init_mod_source_record_storage.sql", jdbcTemplate);
    runSqlScript("/init_sql_functions.sql", jdbcTemplate);
  }

  private static void runSqlScript(String path, JdbcTemplate jdbcTemplate) throws IOException {
    try (var is = BaseDataExportInitializer.class.getResourceAsStream(path)) {
      var sql = Files.read(is, StandardCharsets.UTF_8);
      jdbcTemplate.execute(sql);
    }
  }

  public static LocalDateTimeDeserializer localDateTimeDeserializer = new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .registerModule(new JavaTimeModule().addDeserializer(LocalDateTime.class, localDateTimeDeserializer))
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  private FolioModuleMetadata folioModuleMetadata;
  @Autowired
  private JobExecutionEntityRepository jobExecutionEntityRepository;
  @Autowired
  private ExportIdEntityRepository exportIdEntityRepository;
  @Autowired
  private JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  @Autowired
  private FolioS3Client s3Client;

  public final Map<String, Object> okapiHeaders = new HashMap<>();

  public FolioExecutionContext folioExecutionContext;

  public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
        "spring.datasource.url=" + postgresDBContainer.getJdbcUrl(),
        "spring.datasource.username=" + postgresDBContainer.getUsername(),
        "spring.datasource.password=" + postgresDBContainer.getPassword(),
        "application.remote-files-storage.endpoint=" + MINIO_ENDPOINT,
        "application.remote-files-storage.region=" + REGION,
        "application.remote-files-storage.bucket=" + BUCKET,
        "application.remote-files-storage.accessKey=" + S3_ACCESS_KEY,
        "application.remote-files-storage.secretKey=" + S3_SECRET_KEY,
        "application.remote-files-storage.awsSdk=false");
    }
  }

  @BeforeAll
  static void beforeAll(@Autowired MockMvc mockMvc) {
    setUpTenant(mockMvc);
  }

  @BeforeEach
  void setUp() {
    okapiHeaders.clear();
    okapiHeaders.put(XOkapiHeaders.TENANT, TENANT);
    okapiHeaders.put(XOkapiHeaders.TOKEN, TOKEN);
    okapiHeaders.put(XOkapiHeaders.USER_ID, UUID.randomUUID().toString());

    var localHeaders =
      okapiHeaders.entrySet()
        .stream()
        .filter(e -> e.getKey().startsWith(XOkapiHeaders.OKAPI_HEADERS_PREFIX))
        .collect(Collectors.toMap(Map.Entry::getKey, e -> (Collection<String>) List.of(String.valueOf(e.getValue()))));

    folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, localHeaders);
    s3Client.createBucketIfNotExists();
  }

  @AfterEach
  void eachTearDown() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      exportIdEntityRepository.deleteAll();
      jobExecutionExportFilesEntityRepository.deleteAll();
      jobExecutionEntityRepository.deleteAll();
    }
  }

  @SneakyThrows
  protected static void setUpTenant(MockMvc mockMvc) {
    mockMvc.perform(post("/_/tenant")
      .content(asJsonString(new TenantAttributes().moduleTo("mod-data-export")))
      .headers(defaultHeaders())
      .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
  }

  public static HttpHeaders defaultHeaders() {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.put(XOkapiHeaders.TENANT, List.of(TENANT));
    httpHeaders.add(XOkapiHeaders.TOKEN, TOKEN);
    httpHeaders.add(XOkapiHeaders.USER_ID, UUID.randomUUID().toString());

    return httpHeaders;
  }

  @SneakyThrows
  public static String asJsonString(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }
}
