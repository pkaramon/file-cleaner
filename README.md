# Technologie obiektowe - Projekt

## Setup

### Wymagania systemowe:

- Zainstalowana **Java 17** (lub wyższa) na Twoim komputerze.
- Zainstalowany **Gradle** w wersji kompatybilnej z projektem.
- Zainstalowany **Docker** do uruchomienia kontenera z bazą danych.

### Kroki do uruchomienia projektu:

**1. Jeżeli chcesz używyć innej bazy niż do developmentu, to zmień wartości w pliku `.env` ze zmiennymi
środowiskowymi, który znajduje się w folderze `resources`**.

**2. Skonfiguruj bazę danych:**

- Uruchom kontener z bazą danych PostgreSQL za pomocą Docker Compose.
- W katalogu głównym projektu uruchom następujące polecenie, aby uruchomić kontener z bazą:
  ```bash
  docker-compose up
  ```

**3. Uruchom aplikację:**

  ```bash
  ./gradlew run
  ```

### Zależności:

- Aplikacja wykorzystuje **JavaFX** do budowy interfejsu graficznego.
- Używamy frameworków **Spring Boot** i **Hibernate** do
  zarządzania bazą danych i do wstrzykiwania zalężnośći.
- Projekt używa biblioteki **PostgreSQL** do komunikacji z bazą danych.

## Milestone 1

### Podział pracy

- **Tomasz Kurcoń** - stworzenie bazy danych, implementacja rekursywnego szukania plików
- **Filip Bieńkowski** - stworzenie interfejsu graficznego
- **Piotr Karamon** - konfiguracja Springa, połaczenie Springa z JavaFx, stworzenie encji i funkcjonalności wyszukiwania
  największych plików
- **Jakub Zawistowski** - usuwanie plików (jeszcze nie podpięte pod ui)

### Stan projektu

Aplikacja obecenie pozwala na wybranie katalogu, który chcemy by został zbadany.
Po wybraniu katalogów aplikacja zapisuje dane o plikach do bazy, a następnie
zwraca listę dziesięciu największych plików w danym katalogu.

### Schemat bazy danych

Używamy dwóch tabel jedna dla informacji o plikach druga na logi o wykonywanych akcjach.

![Schemat bazy](docs/db_schema.png)

### Krótki opis najważniejszych klas

- **File** - encja, klasa reprezentująca plik w bazie danych, zawiera podstawowe informacje, takie jak nazwa, rozmiar,
  ścieżka, czas modifikacji.
- **ActionLog** - encja, klasa reprezentująca logi akcji, zawiera informacje:  czas wykonania, opis,
  typ akcji
- **FileService** - serwis, który odpowiada za operacje na plikach, takie jak zapisywanie do bazy danych,
  szukanie największych plików etc.
- **ActionLogRepository** i **FileRepository** - interfejsy, które odpowiadają za komunikację z bazą danych i
  wykonywanie
  jak narazie prostych operacji CRUD.
- **FileSystemService** - abstrakcja nad systemem plików, która pozwala na łatwe testowanie aplikacji.
- **FileSystemServiceImp** - implementacja interfejsu **FileSystemService**.
- **FileListViewController** - kontroler, który odpowiada za obsługę widoku, który wyświetla listę plików.
- **MainViewController** - kontroler, który odpowiada za obsługę widoku głównego, który pozwala na wybór katalogu.
- **LoadLargestFiles** - klasa dziedzicząca z `Task` z JavaFX, która odpowiada za wykonywanie operacji
  znalezienia największych plików w tle.
- **SpringFXMLLoader** - klasa, która pozwala na ładowanie plików FXML z wstrzykiwaniem zależności Springa.

