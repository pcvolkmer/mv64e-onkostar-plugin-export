# Onkostar Plugin "mv64e-onkostar-plugin-export"

Aufgabe dieses Plugins ist es, die Inhalte der DNPM-Formulare in die Datenstruktur des DNPM-Datenmodells 2.1 zu wandeln
und anhand festgelegter Regeln die Notwendigkeit zum Export zu prüfen und diesen durchzuführen.

Hierzu verwendet das Plugin das Projekt https://github.com/pcvolkmer/mv64e-onkostar-data.

**Achtung!**

Version >= 0.2.0 ist für die Verwendung mit DNPM:DIP (api-gateway) Version >= 1.2.3 bestimmt und verwendet Anpassungen
im Geburts- und Sterbedatum im Format 'yyyy-MM' (ohne Tag).

Es ist Onkostar in Version 2.14.0 oder neuer erforderlich, um das Plugin nutzen zu können.

## Einstellungen

Zum Betrieb dieses Plugins ist die Angabe der URL der Zielanwendung erforderlich.

Dies lässt sich initial durch folgende Datenbankanfrage anlegen, später dann in den allgemeinen Einstellungen von
Onkostar auch ändern.

```
INSERT INTO einstellung
  (name, wert, kategorie, beschreibung) 
  VALUES(
    'dnpmexport_url',
    'http://localhost:9000/mtb/etl/patient-record',
    'DNPM',
    'DNPM-Export - URL'
  );

INSERT INTO einstellung
  (name, wert, kategorie, beschreibung)
  VALUES(
    'dnpmexport_prefix',
    'TEST',
    'DNPM',
    'DNPM-Export - Prefix'
  );
```

## Einordnung innerhalb einer DNPM-ETL-Strecke

Dieses Plugin erlaubt das Extrahieren (das "E" in ETL) der benötigten Informationen aus Onkostar und das Übertragen an
die weitere ETL-Strecke.

## Ablauf des Exports

Beim Abschließen eines DNPM-Formulars wird für die Formulare (und damit auch Unterformulare)

* DNPM Klinik/Anamnese
* DNPM Therapieplan
* DNPM FollowUp *(aktuell nicht nicht umgesetzt)*

der Inhalt aller zugehörigen DNPM-Formulare für den Patienten und die Erkrankung, für den/die ein DNPM-Formular
abgeschlossen wurde, ermittelt und zusammengetragen.

Hierbei wird im Falle eines Formulars *DNPM Therapieplan* das entsprechende Formular *DNPM Klinik/Anamnese* ermittelt
und in Folge die zugehörigen Formulare ermittelt:

* **DNPM Klinik/Anamnese** => Ermittlung der Formulare *DNPM Therapieplan* die einen Formularverweis auf dieses Formular
  eingetragen haben.
* **DNPM Therapieplan** => Ermittlung der Formulare *DNPM FollowUp* die einen Formularverweis auf dieses Formular
  eingetragen haben.
  Zudem werden die Formulare *OS.Tumorkonferenz* und *OS.Molekulargenetik* anhand der verwendeten Verweise ermittelt.

Die Übermittlung erfolgt ohne weiteres Zutun von Seiten des Anwenders und wird bereits durch das Abschließen eines
DNPM-Formulars ausgelöst.

## Build

Dieses Projekt verwendet das [Shadow Gradle Plugin](https://gradleup.com/shadow/), um benötigte Abhängigkeiten in die
fertige JAR-Datei zu verpacken.

```bash
./gradlew clean shadowJar
```
