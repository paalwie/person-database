package academy.mischok.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Scanner;

public class Main {

    /*
    TO-DO:
    - mit Spring boot und thymeleaf
    - Code anpassen (schöner, Methoden ordentlich sortieren, etc.
    - Fehlerbehebungen: Exception Handling
    - Gesamte "UI" schöner machen, eine Überschrift etc.
    - In der Aufgabenstellung steht was von einer Tabellenausgabe drin, das anpassen
    - Wenn man einen Eintrag löscht, wird diese ID nicht mehr vergeben, sprich da sind Lücken drin
    - für Zahlen (salary und bonus) eine Abfrage hinzufügen: select * from person where (bsp) salary > x (kleiner, größer und gleich)
     */

    // JDBC URL, username and password (old) Jakubs DB
    //public final static String SERVER = "ep-spring-sunset-a28e3rg5.eu-central-1.aws.neon.tech/neondb?";
    //public final static String USER_AND_DATABASE = "neondb_owner";
    //public final static String PASSWORD = "z0I7jRhUZlmT";

    //meine DB
    public final static String SERVER = "ep-gentle-dew-a2m722pi.eu-central-1.aws.neon.tech/neondb?";
    public final static String USER_AND_DATABASE = "neondb_owner";
    public final static String PASSWORD = "6QyuJaIPBs7f";


    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        try (Connection connection = ConnectionHelper.getConnection()) {
            boolean running = true;
            while (running) {
                printMainMenu();
                String auswahl = scanner.nextLine().toUpperCase();

                switch (auswahl) {
                    case "L":
                        printAllPersons(connection);
                        break;
                    case "A":
                        addPerson(connection, scanner);
                        break;
                    case "E":
                        editPerson(connection, scanner);
                        break;
                    case "D":
                        deletePerson(connection, scanner);
                        break;
                    case "F":
                        filterPersons(connection, scanner);
                        break;
                    case "S":
                        sortPersons(connection, scanner);
                        break;
                    case "X":
                        running = false;
                        System.out.println("Programm beendet.");
                        break;
                    default:
                        System.out.println("Ungültige Eingabe. Bitte erneut versuchen.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //fügt eine neue Person in der Datebank hinzu
    private static void addPerson(Connection connection, Scanner scanner) throws SQLException {
        String firstName = getStringInput(scanner, "Vorname");
        String lastName = getStringInput(scanner, "Nachname");
        String email = getStringInput(scanner, "E-Mail");
        String country = getStringInput(scanner, "Geburtsland");
        System.out.print("Geburtsdatum (JJJJ-MM-TT): ");
        String birthDateString = scanner.nextLine();
        LocalDate birthday = LocalDate.parse(birthDateString);

        //würde auch mit int gehen, aber BigDecimal ist hier besser weil man damit wie bei Strings auf die zugreifen kann (siehe unten)
        BigDecimal salary = new BigDecimal(getStringInput(scanner, "Gehalt"));
        BigDecimal bonus = new BigDecimal(getStringInput(scanner, "Bonus(-Gehalt)"));

        addPerson(connection, firstName, lastName, email, country, birthday, salary, bonus);
        System.out.println("Person erfolgreich hinzugefügt!");
    }

    private static void addPerson(Connection connection, String firstName, String lastName, String email, String country, LocalDate birthday, BigDecimal salary, BigDecimal bonus) throws SQLException {
        //die Fragezeichen bei Insert into werden unten mit dem 1, 2, usw gefüllt
        String insertSQL = "INSERT INTO person (first_name, last_name, email, country, birthday, salary, bonus) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(insertSQL);
        statement.setString(1, firstName);
        statement.setString(2, lastName);
        statement.setString(3, email);
        statement.setString(4, country);
        statement.setObject(5, birthday);
        statement.setBigDecimal(6, salary);
        statement.setBigDecimal(7, bonus);

        statement.executeUpdate();
        System.out.println("Person hinzugefügt!");
    }

    //gibt alle Personen aus
    private static void printAllPersons(Connection connection) throws SQLException {
        String query = "SELECT * FROM person";

        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet resultSet = statement.executeQuery();

        System.out.println("Alle Einträge der 'person' Tabelle:");
        while (resultSet.next()) {
            int id = resultSet.getInt(1);
            String firstName = resultSet.getString(2);
            String lastName = resultSet.getString(3);
            String email = resultSet.getString(4);
            String country = resultSet.getString(5);
            LocalDate birthday = resultSet.getDate(6).toLocalDate();
            BigDecimal salary = resultSet.getBigDecimal(7);
            BigDecimal bonus = resultSet.getBigDecimal(8);

            System.out.printf("ID: %d, Name: %s %s, Email: %s, Land: %s, Geburtstag: %s, Gehalt: %s, Bonus: %s%n",
                    id, firstName, lastName, email, country, birthday, salary, bonus);
        }
    }

    //für das Menü, kann man auch oben einfach so hinzufügen
    private static void printMainMenu() {
        System.out.println("\nWählen Sie eine Aktion:");
        System.out.println("L: Alle Personen anzeigen");
        System.out.println("A: Person hinzufügen");
        System.out.println("E: Person bearbeiten");
        System.out.println("D: Person löschen");
        System.out.println("F: Personen filtern");
        System.out.println("S: Personen sortieren");
        System.out.println("X: Beenden");
    }

    //Int über den Scanner, zum arbeiten
    private static int getIntegerInput(Scanner scanner, String prompt) {
        int input;
        while (!scanner.hasNextInt()) {
            System.out.print(prompt + " (ganze Zahl): ");
            scanner.next();
        }
        input = scanner.nextInt();
        scanner.nextLine();
        return input;
    }

    //String über den Scanner, zum arbeiten
    private static String getStringInput(Scanner scanner, String prompt) {
        System.out.print(prompt + ": ");
        return scanner.nextLine();
    }

    //holt sich die Daten aus der Datenbank per übergebene ID, fragt nach welcher Wert geändert werden soll, überschreibt dann diesen, und speichert die geänderte Daten
    private static void editPerson(Connection connection, Scanner scanner) throws SQLException {

        System.out.print("Geben Sie die ID der zu bearbeitenden Person ein: ");
        int id = getIntegerInput(scanner, "ID der zu bearbeitenden Person");

        String query = "SELECT * FROM person WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, id);
        ResultSet resultSet = statement.executeQuery();

        if (!resultSet.next()) {
            System.out.println("Keine Person mit der ID " + id + " gefunden.");
            return;
        }

        String firstName = resultSet.getString("first_name");
        String lastName = resultSet.getString("last_name");
        String email = resultSet.getString("email");
        String country = resultSet.getString("country");
        LocalDate birthday = resultSet.getDate("birthday").toLocalDate();
        BigDecimal salary = resultSet.getBigDecimal("salary");
        BigDecimal bonus = resultSet.getBigDecimal("bonus");

        boolean weiterBearbeiten = true;
        while (weiterBearbeiten) {
            System.out.println("\nWelche Eigenschaft möchten Sie bearbeiten?");
            System.out.println("1: Vorname (" + firstName + ")");
            System.out.println("2: Nachname (" + lastName + ")");
            System.out.println("3: E-Mail (" + email + ")");
            System.out.println("4: Geburtsland (" + country + ")");
            System.out.println("5: Geburtsdatum (" + birthday + ")");
            System.out.println("6: Gehalt (" + salary + ")");
            System.out.println("7: Bonus (" + bonus + ")");
            System.out.println("0: Abbrechen");

            int auswahl = getIntegerInput(scanner, "Ihre Wahl: ");

            switch (auswahl) {
                case 1:
                    firstName = getStringInput(scanner, "Neuer Vorname (aktuell: " + firstName + "): ");
                    break;
                case 2:
                    lastName = getStringInput(scanner, "Neuer Nachname (aktuell: " + lastName + "): ");
                    break;
                case 3:
                    email = getStringInput(scanner, "Neue E-Mail (aktuell: " + email + "): ");
                    break;
                case 4:
                    country = getStringInput(scanner, "Neues Geburtsland (aktuell: " + country + "): ");
                    break;
                case 5:
                    String newBirthdayString = getStringInput(scanner, "Neues Geburtsdatum (aktuell: " + birthday + ") (JJJJ-MM-TT): ");
                    if (!newBirthdayString.isEmpty()) {
                        birthday = LocalDate.parse(newBirthdayString);
                        //hier: Exception falls man ein ungültiges Datum eingibt
                    }
                    break;
                case 6:
                    String newSalaryString = getStringInput(scanner, "Neues Gehalt (aktuell: " + salary + "): ");
                    if (!newSalaryString.isEmpty()) {
                        salary = new BigDecimal(newSalaryString);
                    }
                    break;
                case 7:
                    String newBonusString = getStringInput(scanner, "Neuer Bonus (aktuell: " + bonus + "): ");
                    if (!newBonusString.isEmpty()) {
                        bonus = new BigDecimal(newBonusString);
                    }
                    break;
                case 0:
                    weiterBearbeiten = false;
                    break;
                default:
                    System.out.println("Ungültige Eingabe. Bitte erneut versuchen.");
            }
        }

        String updateSQL = "UPDATE person SET first_name = ?, last_name = ?, email = ?, country = ?, birthday = ?, salary = ?, bonus = ? WHERE id = ?";
        PreparedStatement updateStatement = connection.prepareStatement(updateSQL);
        updateStatement.setString(1, firstName);
        updateStatement.setString(2, lastName);
        updateStatement.setString(3, email);
        updateStatement.setString(4, country);
        updateStatement.setObject(5, birthday);
        updateStatement.setBigDecimal(6, salary);
        updateStatement.setBigDecimal(7, bonus);
        updateStatement.setInt(8, id);
        updateStatement.executeUpdate();

        System.out.println("Person erfolgreich bearbeitet!");
    }

    //löscht die Person mit der angegebenen ID - Problem: Die ID wird danach nicht mehr vergeben
    private static void deletePerson(Connection connection, Scanner scanner) throws SQLException {
        int id = getIntegerInput(scanner, "ID der zu löschenden Person");

        String query = "DELETE FROM person WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, id);

        int gewählteZeile = statement.executeUpdate();
        if (gewählteZeile == 0) {
            System.out.println("Keine Person mit der ID " + id + " gefunden.");
        } else {
            System.out.println("Person erfolgreich gelöscht.");
        }
    }


    /*
     * filtert die Daten aus der Datenbank per übergebene ID, fragt nach welcher Wert geändert werden soll, überschreibt dann diesen, und speichert die geänderte Daten
     */
    private static void filterPersons(Connection connection, Scanner scanner) throws SQLException {

        //gibt hier über die Konsole den Filterkriterien ein, wie in SQL dargestellt (eventuell ändern, ist nicht wirklich schön)
        //nach 2 Feldern, getrennt durch das = : links das Attribut, rechts den Wert
        System.out.print("Geben Sie die Filterkriterien ein (z.B. last_name=Smith oder country=Germany): ");
        String filterInput = scanner.nextLine();
        String[] filterParts = filterInput.split("=");
        if (filterParts.length != 2) {
            System.out.println("Ungültiges Filterformat. Bitte verwenden Sie das Format feld=wert.");
            return;
        }

        //für Zahlen (salary und bonus) eine Abfrage hinzufügen: select * from person where (bsp) salary > x (kleiner, größer und gleich)
        String field = filterParts[0].trim();
        String value = filterParts[1].trim();
        boolean isStringField = field.equals("first_name") || field.equals("last_name") || field.equals("email") || field.equals("country");

        //schreibt einen SQL Befehl für die Filterung, der den Wert überall sucht, egal ob am Anfang, Mitte oder Schluss (heißt das gesuchte Wort muss irgendwo vorkommen)
        String query = "SELECT * FROM person WHERE ";
        if (isStringField) {
            query += field + " ILIKE ?";
            value = "%" + value + "%";
        } else {
            query += field + " = ?";
        }

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, value);
        ResultSet resultSet = statement.executeQuery();

        System.out.println("Gefilterte Einträge der 'person' Tabelle:");
        while (resultSet.next()) {
            int id = resultSet.getInt(1);
            String firstName = resultSet.getString(2);
            String lastName = resultSet.getString(3);
            String email = resultSet.getString(4);
            String country = resultSet.getString(5);
            LocalDate birthday = resultSet.getDate(6).toLocalDate();
            BigDecimal salary = resultSet.getBigDecimal(7);
            BigDecimal bonus = resultSet.getBigDecimal(8);

            System.out.printf("ID: %d, Name: %s %s, Email: %s, Land: %s, Geburtstag: %s, Gehalt: %s, Bonus: %s%n",
                    id, firstName, lastName, email, country, birthday, salary, bonus);
        }
    }

    //funktioniert ähnlich wie beim Filtern, auch nicht wirklich schön aber funktioniert
    private static void sortPersons(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Geben Sie die Sortierkriterien ein (z.B. last_name ASC oder salary DESC): ");
        String sortInput = scanner.nextLine();
        String[] sortParts = sortInput.split(" ");
        if (sortParts.length != 2) {
            System.out.println("Ungültiges Sortierformat. Bitte verwenden Sie das Format feld richtung.");
            return;
        }

        String attribut = sortParts[0].trim();
        String sortierung = sortParts[1].trim().toUpperCase();
        if (!sortierung.equals("ASC") && !sortierung.equals("DESC")) {
            System.out.println("Ungültige Sortierrichtung. Bitte verwenden Sie ASC oder DESC.");
            return;
        }

        String query = "SELECT * FROM person ORDER BY " + attribut + " " + sortierung;

        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet resultSet = statement.executeQuery();

        System.out.println("Sortierte Einträge der 'person' Tabelle:");
        while (resultSet.next()) {
            int id = resultSet.getInt(1);
            String firstName = resultSet.getString(2);
            String lastName = resultSet.getString(3);
            String email = resultSet.getString(4);
            String country = resultSet.getString(5);
            LocalDate birthday = resultSet.getDate(6).toLocalDate();
            BigDecimal salary = resultSet.getBigDecimal(7);
            BigDecimal bonus = resultSet.getBigDecimal(8);

            System.out.printf("ID: %d, Name: %s %s, Email: %s, Land: %s, Geburtstag: %s, Gehalt: %s, Bonus: %s%n",
                    id, firstName, lastName, email, country, birthday, salary, bonus);
        }
    }
}
