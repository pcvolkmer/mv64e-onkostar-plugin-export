package dev.pcvolkmer.mv64e.dnpmexport;

public class ExportResult {

  public boolean success;
  public String message;

  public ExportResult(boolean success, String message) {
    this.success = success;
    this.message = message;
  }
}
