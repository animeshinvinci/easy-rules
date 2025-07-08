/*
 * The MIT License
 *
 *  Copyright (c) 2020, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package org.jeasy.rules.tutorials.postgresql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

/**
 * Database setup utility for PostgreSQL tutorial.
 * This class sets up an in-memory H2 database for demonstration purposes.
 * In production, you would connect to a real PostgreSQL instance.
 *
 * @author Easy Rules Team
 */
public class DatabaseSetup {

    /**
     * Create and configure a datasource for the tutorial.
     * Uses H2 in PostgreSQL compatibility mode for simplicity.
     * 
     * @return configured datasource
     */
    public static DataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        
        // Use H2 in PostgreSQL mode for tutorial simplicity
        // In production, use actual PostgreSQL connection
        config.setJdbcUrl("jdbc:h2:mem:easyrules;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        
        // Connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        return new HikariDataSource(config);
    }

    /**
     * Run database migrations to set up schema and sample data.
     * 
     * @param dataSource the datasource to migrate
     */
    public static void runMigrations(DataSource dataSource) {
        System.out.println("Setting up database schema and sample data...");
        
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        
        flyway.migrate();
        
        System.out.println("Database setup completed successfully!");
    }

    /**
     * Shutdown the datasource properly.
     * 
     * @param dataSource the datasource to shutdown
     */
    public static void shutdown(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
            System.out.println("Database connection pool closed.");
        }
    }
}