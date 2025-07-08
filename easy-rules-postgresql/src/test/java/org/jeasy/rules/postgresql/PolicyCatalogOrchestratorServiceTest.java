/*
 * The MIT License
 *
 *  Copyright (c) 2021, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
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
package org.jeasy.rules.postgresql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.jeasy.rules.api.Facts;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PolicyCatalogOrchestratorService.
 * Uses Testcontainers to spin up a real PostgreSQL instance for integration testing.
 *
 * @author Easy Rules Team
 */
public class PolicyCatalogOrchestratorServiceTest {

    @ClassRule
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine")
            .withDatabaseName("easyrules_test")
            .withUsername("test")
            .withPassword("test");

    private DataSource dataSource;
    private PolicyCatalogOrchestratorService orchestratorService;

    @Before
    public void setUp() {
        // Configure HikariCP data source
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(5);
        
        dataSource = new HikariDataSource(config);

        // Run Flyway migrations
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        // Create service instance
        orchestratorService = new PolicyCatalogOrchestratorService(dataSource);
    }

    @After
    public void tearDown() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
    }

    @Test
    public void testExecuteRulesWithInsuranceUnderwriting() {
        // Given
        Facts facts = new Facts();
        facts.put("applicant", createApplicant(25, "engineer", 750));

        // When
        RuleExecutionSummary summary = orchestratorService.executeRules("insurance_underwriting", facts);

        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getCatalogName()).isEqualTo("insurance_underwriting");
        assertThat(summary.getTotalRulesCount()).isGreaterThan(0);
        assertThat(summary.getExecutionTimeMs()).isGreaterThan(0);
    }

    @Test
    public void testExecuteRulesWithHighRiskProfession() {
        // Given
        Facts facts = new Facts();
        facts.put("applicant", createApplicant(30, "pilot", 700));

        // When
        RuleExecutionSummary summary = orchestratorService.executeRules("insurance_underwriting", facts);

        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.isSuccessful()).isTrue();
        
        // Verify high risk profession rule was triggered
        assertThat((String) facts.get("riskLevel")).isEqualTo("HIGH");
        assertThat((Boolean) facts.get("requiresManualReview")).isEqualTo(true);
    }

    @Test
    public void testExecuteRulesWithUnderageApplicant() {
        // Given
        Facts facts = new Facts();
        facts.put("applicant", createApplicant(17, "student", 650));

        // When
        RuleExecutionSummary summary = orchestratorService.executeRules("insurance_underwriting", facts);

        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.isSuccessful()).isTrue();
        
        // Age validation rule should not pass for underage applicant
        assertThat((String) facts.get("ageValidationResult")).isNotEqualTo("PASS");
    }

    @Test
    public void testExecuteSpecificRule() {
        // Given
        Facts facts = new Facts();
        facts.put("applicant", createApplicant(25, "engineer", 750));

        // When
        RuleExecutionSummary summary = orchestratorService.executeRule("insurance_underwriting", "age_validation_rule", facts);

        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getTotalRulesCount()).isEqualTo(1);
        assertThat((String) facts.get("ageValidationResult")).isEqualTo("PASS");
    }

    @Test
    public void testExecuteRulesWithNonExistentCatalog() {
        // Given
        Facts facts = new Facts();
        facts.put("test", "value");

        // When
        RuleExecutionSummary summary = orchestratorService.executeRules("non_existent_catalog", facts);

        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.getResult()).isEqualTo("NO_RULES");
        assertThat(summary.getTotalRulesCount()).isEqualTo(0);
    }

    @Test
    public void testExecuteRuleWithNonExistentRule() {
        // Given
        Facts facts = new Facts();
        facts.put("test", "value");

        // When & Then
        assertThatThrownBy(() -> 
            orchestratorService.executeRule("insurance_underwriting", "non_existent_rule", facts))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Rule 'non_existent_rule' not found");
    }

    @Test
    public void testCatalogExists() {
        // When & Then
        assertThat(orchestratorService.catalogExists("insurance_underwriting")).isTrue();
        assertThat(orchestratorService.catalogExists("fraud_detection")).isTrue();
        assertThat(orchestratorService.catalogExists("non_existent_catalog")).isFalse();
    }

    @Test
    public void testCacheOperations() {
        // Given
        Facts facts = new Facts();
        facts.put("applicant", createApplicant(25, "engineer", 750));

        // When - First execution (loads from database)
        RuleExecutionSummary summary1 = orchestratorService.executeRules("insurance_underwriting", facts);
        
        // Then
        assertThat(summary1.isSuccessful()).isTrue();

        // When - Clear cache and execute again
        orchestratorService.clearCache();
        RuleExecutionSummary summary2 = orchestratorService.executeRules("insurance_underwriting", facts);
        
        // Then
        assertThat(summary2.isSuccessful()).isTrue();

        // When - Refresh specific catalog cache
        orchestratorService.refreshCatalogCache("insurance_underwriting");
        RuleExecutionSummary summary3 = orchestratorService.executeRules("insurance_underwriting", facts);
        
        // Then
        assertThat(summary3.isSuccessful()).isTrue();
    }

    @Test
    public void testFraudDetectionRules() {
        // Given
        Facts facts = new Facts();
        facts.put("transaction", createTransaction(50000.0));
        facts.put("account", createAccount(5000.0, 2));

        // When
        RuleExecutionSummary summary = orchestratorService.executeRules("fraud_detection", facts);

        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.isSuccessful()).isTrue();
        assertThat(summary.getTriggeredRulesCount()).isGreaterThan(0);
        
        // Verify fraud detection triggered
        assertThat((String) facts.get("fraudSuspicion")).isEqualTo("HIGH");
    }

    @Test
    public void testNullParameterValidation() {
        Facts facts = new Facts();
        
        // Test null catalog name
        assertThatThrownBy(() -> orchestratorService.executeRules(null, facts))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Catalog name must not be null");
            
        // Test null facts
        assertThatThrownBy(() -> orchestratorService.executeRules("insurance_underwriting", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Facts must not be null");
            
        // Test null rule name
        assertThatThrownBy(() -> orchestratorService.executeRule("insurance_underwriting", null, facts))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Rule name must not be null");
    }

    // Helper methods to create test objects
    private Object createApplicant(int age, String profession, int creditScore) {
        return new TestApplicant(age, profession, creditScore);
    }

    private Object createTransaction(double amount) {
        return new TestTransaction(amount);
    }

    private Object createAccount(double averageMonthlyTransactions, int transactionsToday) {
        return new TestAccount(averageMonthlyTransactions, transactionsToday);
    }

    // Test data classes
    public static class TestApplicant {
        public final int age;
        public final String profession;
        public final int creditScore;

        public TestApplicant(int age, String profession, int creditScore) {
            this.age = age;
            this.profession = profession;
            this.creditScore = creditScore;
        }
    }

    public static class TestTransaction {
        public final double amount;

        public TestTransaction(double amount) {
            this.amount = amount;
        }
    }

    public static class TestAccount {
        public final double averageMonthlyTransactions;
        public final int transactionsToday;

        public TestAccount(double averageMonthlyTransactions, int transactionsToday) {
            this.averageMonthlyTransactions = averageMonthlyTransactions;
            this.transactionsToday = transactionsToday;
        }
    }
}