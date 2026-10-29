package service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Configuração JDBC. Pode ser substituída por variáveis de ambiente. */
public final class Database {
    private static final String URL = System.getenv().getOrDefault("AUTOMOZ_DB_URL",
            "jdbc:mysql://localhost:3306/automoz_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
    private static final String USER = System.getenv().getOrDefault("AUTOMOZ_DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("AUTOMOZ_DB_PASSWORD", "");

    private Database() { }

    public static Connection conectar() throws SQLException {
        try {
            // Dá um diagnóstico claro quando a aplicação é executada sem a dependência Maven no classpath.
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Connector/J nao esta no classpath. Reimporte pom.xml como projeto Maven " +
                    "no IntelliJ e execute novamente.", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static IllegalStateException erro(SQLException e) {
        return new IllegalStateException("Falha de acesso a MySQL. " + e.getMessage() + " Confirme também que o " +
                "MySQL do XAMPP esta iniciado, que database/automoz.sql foi importado e que a configuracao " +
                "AUTOMOZ_DB_URL/USER/PASSWORD esta correta.", e);
    }
}
