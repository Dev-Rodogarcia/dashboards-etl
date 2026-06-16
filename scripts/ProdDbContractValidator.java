import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class ProdDbContractValidator {
    private ProdDbContractValidator() {
    }

    public static void main(String[] args) throws Exception {
        String dbUrl = requiredEnv("DB_URL");
        String dbUser = requiredEnv("DB_USER");
        String dbPassword = requiredEnv("DB_PASSWORD");
        String query = requiredEnv("DASHBOARD_CONTRACT_QUERY");

        DriverManager.setLoginTimeout(10);
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30);
            statement.execute(query);
            System.out.println("[OK] Contrato de localizacao de cargas validado.");
        } catch (SQLException ex) {
            System.err.println("[ERRO] Falha ao validar contrato de banco via JDBC: " + rootMessage(ex));
            throw ex;
        }
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variavel de ambiente obrigatoria ausente: " + name);
        }
        return value;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
