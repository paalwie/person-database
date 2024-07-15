package academy.mischok.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Main {

    //Version 1.05

    /*
    TO-DO:
    - passt
     */

    // JDBC URL, username and password (old) Jakubs DB
    //public final static String SERVER = "ep-spring-sunset-a28e3rg5.eu-central-1.aws.neon.tech/neondb?";
    //public final static String USER_AND_DATABASE = "neondb_owner";
    //public final static String PASSWORD = "z0I7jRhUZlmT";

    //meine DB
    public final static String SERVER = "ep-gentle-dew-a2m722pi.eu-central-1.aws.neon.tech/neondb?";
    public final static String USER_AND_DATABASE = "neondb_owner";
    public final static String PASSWORD = "6QyuJaIPBs7f";

    // Für die rote Farbausgabe
    static final String ANSI_RED = "\u001B[31m";
    static final String ANSI_RESET = "\u001B[0m";
    static final String ANSI_GREEN = "\u001B[32m";

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
                        printAllPersons(connection);
                        editPerson(connection, scanner);
                        break;
                    case "D":
                        printAllPersons(connection);
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
        while (!validateEmail(email)) {
            System.out.println(ANSI_RED + "Ungültige E-Mail-Adresse! Bitte geben Sie eine gültige E-Mail-Adresse im Format 'name@domain.tld' ein." + ANSI_RESET);
            email = scanner.nextLine();
        }

        String country = getStringInput(scanner, "Geburtsland");

        while (!validateCountry(country)) {
            System.out.println(ANSI_RED + "Bitte ein Land ohne Zahlen eintragen" + ANSI_RESET);
            country = scanner.nextLine();
        }

        boolean validInput = false;
        LocalDate birthday = null;
        while (!validInput) {
            System.out.print("Geburtsdatum (JJJJ-MM-TT): ");
            String birthDateString = scanner.nextLine();

            try {
                birthday = LocalDate.parse(birthDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                validInput = true; // Format korrekt, Schleife beenden
            } catch (DateTimeParseException e) {
                System.out.println(ANSI_RED + "Ungültiges Format! Bitte geben Sie das Geburtsdatum im Format JJJJ-MM-TT ein." + ANSI_RESET);
            }
        }

        //würde auch mit int gehen, aber BigDecimal ist hier besser weil man damit wie bei Strings auf die zugreifen kann (siehe unten)
        boolean validInputDecimal = false;
        BigDecimal salary = null;
        while (!validInputDecimal) {
            String salaryString = getStringInput(scanner, "Gehalt"); // Ihre getStringInput-Methode

            try {
                salary = new BigDecimal(salaryString);
                validInputDecimal = true; // Format korrekt, Schleife beenden
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "Ungültige Eingabe! Bitte geben Sie eine Dezimalzahl für das Gehalt ein." + ANSI_RESET);
            }
        }

        boolean validInputBonus = false;
        BigDecimal bonus = null;
        while (!validInputBonus) {
            String bonusString = getStringInput(scanner, "Bonus"); // Ihre getStringInput-Methode

            try {
                bonus = new BigDecimal(bonusString);
                validInputBonus = true; // Format korrekt, Schleife beenden
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "Ungültige Eingabe! Bitte geben Sie eine Dezimalzahl für den Bonus ein." + ANSI_RESET);
            }
        }

        addPerson(connection, firstName, lastName, email, country, birthday, salary, bonus);
        System.out.println(ANSI_GREEN + "Person erfolgreich hinzugefügt!" + ANSI_RESET);
    }

    public static boolean validateEmail(String email) {
        String regex = "^[\\w\\.-]+@([\\w\\.-]+\\.)+[\\w\\.-]{2,3}$";
        return Pattern.matches(regex, email);
    }

    public static boolean validateCountry(String country) {
        String regex = "^[^0-9]*$";
        return Pattern.matches(regex, country);
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
        System.out.println(ANSI_GREEN + "Person hinzugefügt!" + ANSI_RESET);
    }

    private static void printTable(ResultSet resultSet) throws SQLException {

        System.out.format("+------+-----------------+-----------------+--------------------------------+---------------------------+------------+------------+----------+%n");
        System.out.format("|  ID  |    Vorname      |    Nachname     |             E-Mail             |           Land            | Geburtstag |   Gehalt   |   Bonus  |%n");
        System.out.format("+------+-----------------+-----------------+--------------------------------+---------------------------+------------+------------+----------+%n");
        String leftAlignment = "| %4d | %-15s | %-15s | %-30s | %-25s | %-10s | %-10s | %-8s |%n";
        while (resultSet.next()) {
            System.out.format(leftAlignment,
                    resultSet.getInt(1),
                    resultSet.getString(2),
                    resultSet.getString(3),
                    resultSet.getString(4),
                    resultSet.getString(5),
                    resultSet.getDate(6).toLocalDate(),
                    resultSet.getBigDecimal(7),
                    resultSet.getBigDecimal(8));
            System.out.format("+------+-----------------+-----------------+--------------------------------+---------------------------+------------+------------+----------+%n");
        }
    }

    //gibt alle Personen aus
    private static void printAllPersons(Connection connection) throws SQLException {
        String query = "SELECT * FROM person";

        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet resultSet = statement.executeQuery();

        System.out.println("Alle Einträge der 'person' Tabelle:");

        printTable(resultSet);
    }

    //für das Menü, kann man auch oben einfach so hinzufügen
    private static void printMainMenu() {
        System.out.println("+-------------------------------+");
        System.out.println("|    Wählen Sie eine Aktion:    |");
        System.out.println("+------+------------------------+");
        System.out.println("|  L:  | Alle Personen anzeigen |");
        System.out.println("+------+------------------------+");
        System.out.println("|  A:  | Person hinzufügen      |");
        System.out.println("+------+------------------------+");
        System.out.println("|  E:  | Person bearbeiten      |");
        System.out.println("+------+------------------------+");
        System.out.println("|  D:  | Person löschen         |");
        System.out.println("+------+------------------------+");
        System.out.println("|  F:  | Personen filtern       |");
        System.out.println("+------+------------------------+");
        System.out.println("|  S:  | Personen sortieren     |");
        System.out.println("+------+------------------------+");
        System.out.println("|  X:  | Beenden                |");
        System.out.println("+------+------------------------+");
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
            System.out.println(ANSI_RED + "Keine Person mit der ID " + id + " gefunden." + ANSI_RESET);
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

        System.out.println(ANSI_GREEN + "Person erfolgreich bearbeitet!" + ANSI_RESET);
    }

    //löscht die Person mit der angegebenen ID - Problem: Die ID wird danach nicht mehr vergeben
    private static void deletePerson(Connection connection, Scanner scanner) throws SQLException {

        String bestätigung = " ";
        System.out.print("Geben Sie die ID der zu löschenden Person ein: ");
        int id = getIntegerInput(scanner, "ID der zu löschenden Person");

        System.out.println("Sind Sie sich sicher das sie die ID " + id + " löschen wollen ? ");

        if (scanner.nextLine().equalsIgnoreCase("y")) {

            String query = "DELETE FROM person WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, id);

            int gewählteZeile = statement.executeUpdate();
            if (gewählteZeile == 0) {
                System.out.println("Keine Person mit der ID " + id + " gefunden.");
            } else {
                System.out.println(ANSI_GREEN + "Person erfolgreich gelöscht." + ANSI_RESET);
            }
        } else {
            System.out.println(ANSI_RED + "Löschvorgang wurde wie gewünscht abgebrochen" + ANSI_RESET);

        }

    }

    /*
     * filtert die Daten aus der Datenbank per übergebene ID, fragt nach welcher Wert geändert werden soll, überschreibt dann diesen, und speichert die geänderte Daten
     */
    private static void filterPersons(Connection connection, Scanner scanner) throws SQLException {

        String field;
        String value = "";
        String query = "";

        //gibt hier über die Konsole den Filterkriterien ein, wie in SQL dargestellt (eventuell ändern, ist nicht wirklich schön)
        //nach 2 Feldern, getrennt durch das = : links das Attribut, rechts den Wert
        System.out.println("Was möchten Sie machen?");
        System.out.println("Geben Sie ein: 1 -> Filtern nach Vornamen, Nachname, Land, Email -> sucht ob diese Eingabe in der gewünschten Spalte vorkommt");
        System.out.println("Geben Sie ein: 2 -> Filtert Gehalt und Bonus danach, ob dieser Wert exakt vorkommt");

        switch (getIntegerInput(scanner, "Ihre Wahl")) {
            case 1:
                System.out.print("Geben Sie die Filterkriterien ein (z.B. last_name=Smith oder country=Germany): ");
                String filterInput = scanner.nextLine();
                String[] filterParts = filterInput.split("=");
                if (filterParts.length != 2) {
                    System.out.println("Ungültiges Filterformat. Bitte verwenden Sie das Format feld=wert.");
                    return;
                }

                //für Zahlen (salary und bonus) eine Abfrage hinzufügen: select * from person where (bsp) salary > x (kleiner, größer und gleich)
                field = filterParts[0].trim();
                value = filterParts[1].trim();
                boolean isStringField = field.equals("first_name") || field.equals("last_name") || field.equals("email") || field.equals("country");

                //schreibt einen SQL Befehl für die Filterung, der den Wert überall sucht, egal ob am Anfang, Mitte oder Schluss (heißt das gesuchte Wort muss irgendwo vorkommen)
                query = "SELECT * FROM person WHERE ";
                if (isStringField) {
                    query += field + " ILIKE ?";
                    value = "%" + value + "%";
                } else {
                    query += field + " = ?";
                }
                break;
            case 2:
                System.out.print("Geben Sie die Filterkriterien ein (z.B. salary=1000.00 oder bonus=500.00): ");
                String filterInput2 = scanner.nextLine();
                String[] filterParts2 = filterInput2.split("=");
                if (filterParts2.length != 2) {
                    System.out.println("Ungültiges Filterformat. Bitte verwenden Sie das Format feld=wert.");
                    return;
                }
                field = filterParts2[0].trim();
                value = filterParts2[1].trim();
                query = "SELECT * FROM person WHERE " + field + " = CAST(? AS INTEGER)";
                System.out.println(query);
                break;
            default:
                System.out.println("Ungültige Eingabe. Bitte erneut versuchen.");
        }

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, value);
        ResultSet resultSet = statement.executeQuery();

        System.out.println("Gefilterte Einträge der 'person' Tabelle:");

        printTable(resultSet);

        System.out.println("Möchten Sie das Ergebnis noch sortieren? (y/n)");
        if (scanner.nextLine().equalsIgnoreCase("y")) {

            sortierenNachFiltern(connection, scanner, statement.toString());
        }

    }


    //funktioniert ähnlich wie beim Filtern, auch nicht wirklich schön aber funktioniert
    private static void sortPersons(Connection connection, Scanner scanner) throws SQLException {

        String spalte;
        String richtung;

        System.out.println("Geben Sie die Nummer für die Sortierspalte ein: ");
        System.out.println("1 -> Vorname");
        System.out.println("2 -> Nachname");
        System.out.println("3 -> E-Mail");
        System.out.println("4 -> Land");
        System.out.println("5 -> Geburtstag");
        System.out.println("6 -> Gehalt");
        System.out.println("7 -> Bonus");

        switch (scanner.nextLine()) {
            case "1":
                spalte = "LOWER(first_name)";
                break;
            case "2":
                spalte = "LOWER(last_name)";
                break;
            case "3":
                spalte = "LOWER(email)";
                break;
            case "4":
                spalte = "LOWER(country)";
                break;
            case "5":
                spalte = "birthday";
                break;
            case "6":
                spalte = "salary";
                break;
            case "7":
                spalte = "bonus";
                break;
            default:
                System.out.println("Ungültiges Sortierformat. Bitte verwenden Sie das Format feld richtung.");
                return;
        }

        System.out.println("Geben Sie die Nummer für die Sortierrichtung ein: ");
        System.out.println("1 -> Aufsteigend");
        System.out.println("2 -> Absteigend");


        switch (scanner.nextLine()) {
            case "1":
                richtung = "ASC";
                break;
            case "2":
                richtung = "DESC";
                break;
            default:
                System.out.println("Ungültiges Sortierformat. Bitte verwenden Sie das Format feld richtung.");
                return;
        }

        String query = "SELECT * FROM person ORDER BY " + spalte + " " + richtung;

        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet resultSet = statement.executeQuery();

        System.out.println("Sortierte Einträge der 'person' Tabelle:");

        printTable(resultSet);
    }

    private static void sortierenNachFiltern(Connection connection, Scanner scanner, String query) throws SQLException {

        String spalte;
        String richtung;

        System.out.println("Geben Sie die Nummer für die Sortierspalte ein: ");
        System.out.println("1 -> Vorname");
        System.out.println("2 -> Nachname");
        System.out.println("3 -> E-Mail");
        System.out.println("4 -> Land");
        System.out.println("5 -> Geburtstag");
        System.out.println("6 -> Gehalt");
        System.out.println("7 -> Bonus");

        switch (scanner.nextLine()) {
            case "1":
                spalte = "LOWER(first_name)";
                break;
            case "2":
                spalte = "LOWER(last_name)";
                break;
            case "3":
                spalte = "LOWER(email)";
                break;
            case "4":
                spalte = "LOWER(country)";
                break;
            case "5":
                spalte = "birthday";
                break;
            case "6":
                spalte = "salary";
                break;
            case "7":
                spalte = "bonus";
                break;
            default:
                System.out.println("Ungültiges Sortierformat. Bitte verwenden Sie das Format feld richtung.");
                return;
        }

        System.out.println("Geben Sie die Nummer für die Sortierrichtung ein: ");
        System.out.println("1 -> Aufsteigend");
        System.out.println("2 -> Absteigend");


        switch (scanner.nextLine()) {
            case "1":
                richtung = "ASC";
                break;
            case "2":
                richtung = "DESC";
                break;
            default:
                System.out.println("Ungültiges Sortierformat. Bitte verwenden Sie das Format feld richtung.");
                return;
        }

        query += " ORDER BY " + spalte + " " + richtung;

        System.out.println(query);

        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet resultSet = statement.executeQuery();

        System.out.println("Sortierte Einträge der 'person' Tabelle:");

        printTable(resultSet);


    }
}
