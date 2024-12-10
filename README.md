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
  zarządzania bazą danych i do wstrzykiwania zależności.
- Projekt używa biblioteki **PostgreSQL** do komunikacji z bazą danych.

## Milestone 1

### Podział pracy

- **Tomasz Kurcoń** - stworzenie bazy danych, implementacja rekursywnego szukania plików, ulepszenie połączenia Springa i JavaFx
- **Filip Bieńkowski** - stworzenie interfejsu graficznego
- **Piotr Karamon** - konfiguracja Springa, połaczenie Springa z JavaFx, stworzenie encji i funkcjonalności wyszukiwania
  największych plików, wielowątkowość w ui
- **Jakub Zawistowski** - usuwanie plików

### Stan projektu

Aplikacja obecenie pozwala na wybranie katalogu, który chcemy by został zbadany.
Po wybraniu katalogu aplikacja zapisuje dane o plikach do bazy, a następnie
wyświetla listę znalezionych plików w postaci tabeli.
Możemy potem wyświetlić 10 największych, lub wyświetlić wszystkie.
Możemy również usunąć zaznaczone przez nas pliki.

### Schemat bazy danych

Używamy dwóch tabel - jedna przechowywuje informacje o plikach, a druga logi o wykonywanych akcjach.

![Schemat bazy](docs/db_schema.png)

### Krótki opis najważniejszych klas

- **File** - encja, klasa reprezentująca plik w bazie danych, zawiera podstawowe informacje, takie jak nazwa, rozmiar,
  ścieżka, data ostatniej modifikacji.
- **ActionLog** - encja, klasa reprezentująca logi akcji, zawiera następujące informacje:  czas wykonania, opis,
  typ akcji.
- **FileService** - serwis, który odpowiada za operacje na plikach, takie jak zapisywanie do bazy danych,
  szukanie największych plików etc.
- **ActionLogRepository** i **FileRepository** - interfejsy, które odpowiadają za komunikację z bazą danych i
  wykonywanie
  jak na razie prostych operacji CRUD.
- **FileSystemService** - abstrakcja nad systemem plików, która pozwala na łatwe testowanie aplikacji.
- **FileSystemServiceImp** - implementacja interfejsu **FileSystemService**.
- **FileListViewController** - kontroler, który odpowiada za obsługę widoku, który wyświetla listę plików.
- **MainViewController** - kontroler, który odpowiada za obsługę widoku głównego, który pozwala na wybór katalogu.
- **BackgroundTask** - klasa dziedzicząca z `Task` z JavaFX, pozwala w łatwy sposób przenosić zapytania do bazy i
    inne długie zadania
    na inny wątek, niż ten odpowiedzialny za ui. 
- **SpringFXMLLoader** - klasa, która pozwala na ładowanie plików FXML z wstrzykiwaniem zależności Springa.

