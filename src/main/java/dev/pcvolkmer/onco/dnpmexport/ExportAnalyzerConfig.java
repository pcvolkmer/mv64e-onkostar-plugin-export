package dev.pcvolkmer.onco.dnpmexport;

import dev.pcvolkmer.onco.datamapper.mapper.MtbDataMapper;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ExportAnalyzerConfig {

  @Bean
  public MtbDataMapper mtbDataMapper(final DataSource dataSource) {
    // Reuse default Onkostar DataSource for MtbDataMapper
    return MtbDataMapper.create(dataSource);
  }

  @Bean(name = "mv64eExportRestTemplate")
  public RestTemplate restTemplate() {
    final var restTemplate = new RestTemplate();
    restTemplate
        .getMessageConverters()
        .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
    return restTemplate;
  }
}
