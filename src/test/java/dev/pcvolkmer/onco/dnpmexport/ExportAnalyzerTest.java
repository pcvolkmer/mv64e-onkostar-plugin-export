package dev.pcvolkmer.onco.dnpmexport;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import de.itc.onkostar.api.IOnkostarApi;
import de.itc.onkostar.api.Item;
import de.itc.onkostar.api.Procedure;
import dev.pcvolkmer.mv64e.mtb.Mtb;
import dev.pcvolkmer.onco.datamapper.mapper.MtbDataMapper;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class ExportAnalyzerTest {

  private IOnkostarApi onkostarApi;
  private MtbDataMapper mtbDataMapper;
  private RestTemplate restTemplate;

  private ExportAnalyzer analyzer;

  @BeforeEach
  void setup(
      @Mock IOnkostarApi onkostarApi,
      @Mock MtbDataMapper mtbDataMapper,
      @Mock RestTemplate restTemplate) {
    this.onkostarApi = onkostarApi;
    this.mtbDataMapper = mtbDataMapper;
    this.restTemplate = restTemplate;
    this.analyzer = new ExportAnalyzer(onkostarApi, mtbDataMapper, restTemplate);

    doAnswer(
            invocationOnMock -> {
              var name = invocationOnMock.getArgument(0, String.class);
              return defaultSetting(name);
            })
        .when(this.onkostarApi)
        .getGlobalSetting(anyString());
  }

  private static String defaultSetting(String name) {
    switch (name) {
      case "dnpmexport_url":
        return "http://localhost:9000/mtb/etl/patient-record";
      case "dnpmexport_prefix":
        return "TEST";
      default:
        return null;
    }
  }

  @Test
  void shouldExtractMtbDataForKlinikAnamnese() {
    when(mtbDataMapper.getByCaseId(anyString())).thenReturn(Mtb.builder().build());
    when(this.restTemplate.postForEntity(any(URI.class), any(), any()))
        .thenReturn(ResponseEntity.accepted().build());

    var procedure = new Procedure(onkostarApi);
    procedure.setId(1);
    procedure.setFormName("DNPM Klinik/Anamnese");
    procedure.setValue("FallnummerMV", new Item("FallnummerMV", "1600012345"));

    this.analyzer.analyze(procedure, null);

    var caseIdCaptor = ArgumentCaptor.forClass(String.class);
    verify(mtbDataMapper, times(1)).getByCaseId(caseIdCaptor.capture());
    assertThat(caseIdCaptor.getValue()).isEqualTo("1600012345");
  }

  @Test
  void shouldExtractMtbDataForTherapieplan() {
    doAnswer(
            invocationOnMock -> {
              var procedure = new Procedure(onkostarApi);
              procedure.setId(1);
              procedure.setFormName("DNPM Klinik/Anamnese");
              procedure.setValue("FallnummerMV", new Item("FallnummerMV", "1600012345"));
              return procedure;
            })
        .when(this.onkostarApi)
        .getProcedure(anyInt());

    when(mtbDataMapper.getByCaseId(anyString())).thenReturn(Mtb.builder().build());
    when(this.restTemplate.postForEntity(any(URI.class), any(), any()))
        .thenReturn(ResponseEntity.accepted().build());

    var procedure = new Procedure(onkostarApi);
    procedure.setId(2);
    procedure.setFormName("DNPM Therapieplan");
    procedure.setValue("refdnpmklinikanamnese", new Item("ref_dnpm_klinikanamnese", 1));

    this.analyzer.analyze(procedure, null);

    var kpaIdCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(onkostarApi, times(1)).getProcedure(kpaIdCaptor.capture());
    assertThat(kpaIdCaptor.getValue()).isEqualTo(1);

    var caseIdCaptor = ArgumentCaptor.forClass(String.class);
    verify(mtbDataMapper, times(1)).getByCaseId(caseIdCaptor.capture());
    assertThat(caseIdCaptor.getValue()).isEqualTo("1600012345");
  }

  @Test
  void shouldSendHttpRequestWithMtbData() {
    when(mtbDataMapper.getByCaseId(anyString())).thenReturn(Mtb.builder().build());
    when(this.restTemplate.postForEntity(any(URI.class), any(), any()))
        .thenReturn(ResponseEntity.accepted().build());

    var procedure = new Procedure(onkostarApi);
    procedure.setId(1);
    procedure.setFormName("DNPM Klinik/Anamnese");
    procedure.setValue("FallnummerMV", new Item("FallnummerMV", "1600012345"));

    this.analyzer.analyze(procedure, null);

    verify(restTemplate, times(1)).postForEntity(any(URI.class), any(), any());
  }
}
