package dev.pcvolkmer.mv64e.dnpmexport;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.itc.onkostar.api.Disease;
import de.itc.onkostar.api.IOnkostarApi;
import de.itc.onkostar.api.Procedure;
import de.itc.onkostar.api.analysis.AnalyseTriggerEvent;
import de.itc.onkostar.api.analysis.AnalyzerRequirement;
import de.itc.onkostar.api.analysis.IProcedureAnalyzer;
import de.itc.onkostar.api.analysis.OnkostarPluginType;
import dev.pcvolkmer.mv64e.datamapper.mapper.MtbDataMapper;
import dev.pcvolkmer.mv64e.mtb.Converter;
import dev.pcvolkmer.mv64e.mtb.Mtb;
import java.net.URI;
import java.util.Base64;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Analyzer to start export if DNPM Klinik/Anamnese or DNPM Therapieplan form gets locked
 *
 * @author Paul-Christian Volkmer
 * @since 0.1
 */
@Component
public class ExportAnalyzer implements IProcedureAnalyzer {

  private static final Logger logger = LoggerFactory.getLogger(ExportAnalyzer.class);

  private final IOnkostarApi onkostarApi;
  private final MtbDataMapper mtbDataMapper;
  private final RestTemplate restTemplate;

  public ExportAnalyzer(
      final IOnkostarApi onkostarApi,
      final MtbDataMapper mtbDataMapper,
      @Qualifier("mv64eExportRestTemplate") final RestTemplate restTemplate) {
    this.onkostarApi = onkostarApi;
    this.mtbDataMapper = mtbDataMapper;
    this.restTemplate = restTemplate;
  }

  @Override
  public OnkostarPluginType getType() {
    return OnkostarPluginType.ANALYZER;
  }

  @Override
  public String getVersion() {
    return "0.4.5"; // x-release-please-version
  }

  @Override
  public String getName() {
    return "DNPM-Export-Plugin";
  }

  @Override
  public String getDescription() {
    return "OS Export-Plugin für das DNPM-Datenmodell 2.1";
  }

  @Override
  public boolean isSynchronous() {
    return false;
  }

  @Override
  public AnalyzerRequirement getRequirement() {
    return AnalyzerRequirement.PROCEDURE;
  }

  @Override
  public boolean isRelevantForDeletedProcedure() {
    return false;
  }

  @Override
  public boolean isRelevantForAnalyzer(final Procedure procedure, final Disease disease) {
    return null != procedure
        && ("DNPM Klinik/Anamnese".equals(procedure.getFormName())
            || "DNPM Therapieplan".equals(procedure.getFormName()));
  }

  @Override
  public void analyze(final Procedure procedure, final Disease disease) {
    logger.info("Starting export for procedure {}", procedure.getId());

    try {
      String caseId;
      switch (procedure.getFormName()) {
        case "DNPM Klinik/Anamnese":
          caseId = procedure.getValue("FallnummerMV").getString();
          break;
        case "DNPM Therapieplan":
          var kpaProcedure =
              onkostarApi.getProcedure(procedure.getValue("refdnpmklinikanamnese").getInt());
          caseId = kpaProcedure.getValue("FallnummerMV").getString();
          break;
        default:
          logger.info("Cannot handle procedure form {}", procedure.getFormName());
          return;
      }

      var mtb = mtbDataMapper.getByCaseId(caseId);
      sendMtbFileRequest(mtb);
    } catch (Exception e) {
      logger.error("Cannot export mtb data for procedure {}", procedure.getId(), e);
    }
  }

  @Override
  public Set<AnalyseTriggerEvent> getTriggerEvents() {
    return Set.of(
        AnalyseTriggerEvent.LOCK, AnalyseTriggerEvent.EDIT_LOCK, AnalyseTriggerEvent.REORG);
  }

  private void sendMtbFileRequest(Mtb mtb) {
    var exportUrl = onkostarApi.getGlobalSetting("dnpmexport_url");

    try {
      var uri = URI.create(exportUrl);
      var headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      if (uri.getUserInfo() != null) {
        headers.set(
            HttpHeaders.AUTHORIZATION,
            "Basic " + Base64.getEncoder().encodeToString(uri.getUserInfo().getBytes()));
      }

      final var input = Converter.toJsonString(mtb);
      final var jsonBuilder = new StringBuilder(input.length());
      input
          .codePoints()
          .filter(Character::isValidCodePoint)
          .filter(cp -> !Character.isSurrogate((char) cp))
          .forEach(jsonBuilder::appendCodePoint);

      var entityReq = new HttpEntity<>(jsonBuilder.toString(), headers);

      var r = restTemplate.postForEntity(uri, entityReq, String.class);
      if (!r.getStatusCode().is2xxSuccessful()) {
        logger.error("Error sending to remote system: {}", r.getBody());
        throw new RuntimeException("Kann Daten nicht an das externe System senden");
      }
    } catch (IllegalArgumentException e) {
      logger.error("Not a valid URI to export to: '{}'", exportUrl);
      throw new RuntimeException("Keine gültige Adresse für das externe System");
    } catch (JsonProcessingException e) {
      logger.error("Cannot send data to remote system", e);
      throw new RuntimeException("Kann Daten nicht in JSON-String wandeln");
    } catch (HttpClientErrorException e) {
      logger.error("Cannot send data to remote system", e);
      logger.error("Response: {}", e.getResponseBodyAsString());
      throw new RuntimeException("Kann Daten nicht an das externe System senden");
    } catch (RestClientException e) {
      logger.error("Cannot send data to remote system", e);
      throw new RuntimeException("Kann Daten nicht an das externe System senden");
    }
  }
}
