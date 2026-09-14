package test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.TableResult;


public final class SqlRunner {

    private static final String PARAM_OPTION = "--param";

    private SqlRunner() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception e) {
            System.err.println("SQL application failed: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("SQL application failed", e);
        }
    }

    static void run(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException(
                    "Usage: SqlRunner <sql-file> [--param NAME=value ...]");
        }

        Path sqlFile = Path.of(args[0]);
        if (!Files.isRegularFile(sqlFile) || !Files.isReadable(sqlFile)) {
            throw new IllegalArgumentException(
                    "SQL file does not exist or is not readable: "
                            + sqlFile.toAbsolutePath());
        }

        Map<String, String> parameters = parseParameters(args);

        System.out.println("Reading SQL file: " + sqlFile.toAbsolutePath());

        if (!parameters.isEmpty()) {
            System.out.println("SQL parameters:");
            parameters.forEach((name, value) ->
                    System.out.println("  " + name + "=" + value));
        }

        String sql = readSql(sqlFile);
        sql = substituteParameters(sql, parameters);

        List<String> statements = splitStatements(sql);
        if (statements.isEmpty()) {
            throw new IllegalArgumentException(
                    "SQL file contains no executable statements");
        }

        if (!startsWithKeyword(
                statements.get(statements.size() - 1), "INSERT")) {
            throw new IllegalArgumentException(
                    "The final SQL statement must be INSERT INTO");
        }

        System.out.println(
                "Parsed " + statements.size() + " SQL statements");

        TableEnvironment tableEnvironment =
                TableEnvironment.create(
                        EnvironmentSettings.newInstance().build());

        for (int index = 0; index < statements.size(); index++) {
            String statement = statements.get(index);
            boolean finalStatement =
                    index == statements.size() - 1;

            System.out.printf(
                    "Executing statement %d/%d:%n%s%n",
                    index + 1,
                    statements.size(),
                    statement);

            try {
                TableResult result =
                        tableEnvironment.executeSql(statement);

                if (finalStatement) {
                    System.out.println(
                            "Insert submitted; waiting for the job to finish");
                    result.await();
                } else if (startsWithKeyword(
                        statement, "CREATE TABLE")) {
                    System.out.println(
                            "Table registered successfully");
                } else {
                    System.out.println(
                            "Statement executed successfully");
                }
            } catch (Exception e) {
                throw new RuntimeException(
                        "Statement "
                                + (index + 1)
                                + " failed: "
                                + statement,
                        e);
            }
        }
    }

    private static Map<String, String> parseParameters(String[] args) {
        Map<String, String> parameters = new LinkedHashMap<>();

        int index = 1;

        while (index < args.length) {
            if (!PARAM_OPTION.equals(args[index])) {
                throw new IllegalArgumentException(
                        "Unexpected argument: "
                                + args[index]
                                + ". Expected --param NAME=value");
            }

            if (index + 1 >= args.length) {
                throw new IllegalArgumentException(
                        "--param must be followed by NAME=value");
            }

            String parameter = args[index + 1];
            int separator = parameter.indexOf('=');

            if (separator <= 0) {
                throw new IllegalArgumentException(
                        "Invalid parameter: "
                                + parameter
                                + ". Expected NAME=value");
            }

            String name = parameter.substring(0, separator);
            String value = parameter.substring(separator + 1);

            if (name.isBlank()) {
                throw new IllegalArgumentException(
                        "Parameter name must not be empty");
            }

            if (parameters.put(name, value) != null) {
                throw new IllegalArgumentException(
                        "Duplicate parameter: " + name);
            }

            index += 2;
        }

        return parameters;
    }

    private static String substituteParameters(
            String sql,
            Map<String, String> parameters) {

        String result = sql;

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue());
        }

        int unresolvedStart = result.indexOf("${");
        if (unresolvedStart >= 0) {
            int unresolvedEnd =
                    result.indexOf('}', unresolvedStart);

            if (unresolvedEnd > unresolvedStart) {
                String placeholder =
                        result.substring(
                                unresolvedStart,
                                unresolvedEnd + 1);

                throw new IllegalArgumentException(
                        "No value provided for SQL parameter "
                                + placeholder);
            }
        }

        return result;
    }

    private static String readSql(Path sqlFile) throws IOException {
        return Files.readString(
                sqlFile,
                StandardCharsets.UTF_8);
    }

    static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;

        for (int index = 0; index < sql.length(); index++) {
            char character = sql.charAt(index);

            if (character == '\''
                    && inSingleQuote
                    && index + 1 < sql.length()
                    && sql.charAt(index + 1) == '\'') {
                current.append("''");
                index++;
                continue;
            }

            if (character == '\'') {
                inSingleQuote = !inSingleQuote;
                current.append(character);
                continue;
            }

            if (character == ';' && !inSingleQuote) {
                addIfNotBlank(statements, current);
                continue;
            }

            current.append(character);
        }

        if (inSingleQuote) {
            throw new IllegalArgumentException(
                    "SQL contains an unterminated single-quoted string");
        }

        addIfNotBlank(statements, current);
        return statements;
    }

    private static void addIfNotBlank(
            List<String> statements,
            StringBuilder current) {

        String statement = current.toString().trim();

        if (!statement.isEmpty()) {
            statements.add(statement);
        }

        current.setLength(0);
    }

    private static boolean startsWithKeyword(
            String statement,
            String keyword) {

        return statement
                .toUpperCase(Locale.ROOT)
                .startsWith(keyword);
    }
}