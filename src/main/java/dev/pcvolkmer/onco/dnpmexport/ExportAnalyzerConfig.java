package dev.pcvolkmer.onco.dnpmexport;

import dev.pcvolkmer.onco.datamapper.mapper.MtbDataMapper;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    return new RestTemplate();
  }
}
