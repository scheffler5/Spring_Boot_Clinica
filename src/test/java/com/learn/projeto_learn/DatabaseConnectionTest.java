package com.learn.projeto_learn;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Deve conectar ao banco de dados PostgreSQL com sucesso")
    void testDatabaseConnection() throws SQLException {

        assertThat(dataSource).isNotNull();

        try (Connection connection = dataSource.getConnection()) {

            assertThat(connection).isNotNull();
            assertThat(connection.isValid(2)).isTrue();

            System.out.println("✅ CONEXÃO COM O BANCO BEM SUCEDIDA!");
            System.out.println("📦 Banco: " + connection.getMetaData().getDatabaseProductName());
            System.out.println("🔗 URL: " + connection.getMetaData().getURL());
        }
    }
}
