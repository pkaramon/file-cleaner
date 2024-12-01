# Technologie obiektowe - Projekt

## Setup

### Wymagania systemowe:
- Zainstalowana **Java 17** (lub wyższa) na Twoim komputerze.
- Zainstalowany **Gradle** w wersji kompatybilnej z projektem.
- Zainstalowany **Docker** do uruchomienia kontenera z bazą danych.

### Kroki do uruchomienia projektu:

**1. Skonfiguruj bazę danych:**
   - Uruchom kontener z bazą danych PostgreSQL za pomocą Docker Compose.
   - W katalogu głównym projektu uruchom następujące polecenie, aby uruchomić kontener z bazą:
     ```bash
     docker-compose up
     ```

### Zależności:
- Projekt używa biblioteki **PostgreSQL** do łączenia z bazą danych.
- Aplikacja wykorzystuje **JavaFX** do budowy interfejsu graficznego.

