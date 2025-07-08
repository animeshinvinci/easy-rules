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
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PostgreSQLRuleFactory.
 *
 * @author Easy Rules Team
 */
public class PostgreSQLRuleFactoryTest {

    @ClassRule
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine")
            .withDatabaseName("easyrules_test")
            .withUsername("test")
            .withPassword("test");

    private DataSource dataSource;
    private PostgreSQLRuleFactory ruleFactory;

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

        // Create factory instance
        ruleFactory = new PostgreSQLRuleFactory(dataSource);
    }

    @After
    public void tearDown() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
    }

    @Test
    public void testCreateRuleFromDatabase() {
        // When
        Rule rule = ruleFactory.createRule("insurance_underwriting", "age_validation_rule");

        // Then
        assertThat(rule).isNotNull();
        assertThat(rule.getName()).isEqualTo("age_validation_rule");
        assertThat(rule.getDescription()).isEqualTo("Validate applicant age for insurance");
        assertThat(rule.getPriority()).isEqualTo(100);
    }

    @Test
    public void testCreateRuleExecutesCorrectly() {
        // Given
        Rule rule = ruleFactory.createRule("insurance_underwriting", "age_validation_rule");
        Facts facts = new Facts();
        facts.put("applicant", new TestApplicant(25, "engineer"));

        // When
        boolean shouldFire = rule.evaluate(facts);

        // Then
        assertThat(shouldFire).isTrue();

        // When
        try {
            rule.execute(facts);
        } catch (Exception e) {
            fail("Rule execution should not throw exception", e);
        }

        // Then
        assertThat((String) facts.get("ageValidationResult")).isEqualTo("PASS");
    }

    @Test
    public void testCreateRulesFromCatalog() {
        // When
        Rules rules = ruleFactory.createRules("insurance_underwriting");

        // Then
        assertThat(rules).isNotNull();
        assertThat(rules.size()).isGreaterThan(0);
        
        // Verify rules are sorted by priority
        Rule previousRule = null;
        for (Rule rule : rules) {
            if (previousRule != null) {
                assertThat(rule.getPriority()).isGreaterThanOrEqualTo(previousRule.getPriority());
            }
            previousRule = rule;
        }
    }

    @Test
    public void testCreateRuleWithHighRiskProfession() {
        // Given
        Rule rule = ruleFactory.createRule("insurance_underwriting", "high_risk_profession_rule");
        Facts facts = new Facts();
        facts.put("applicant", new TestApplicant(30, "pilot"));

        // When
        boolean shouldFire = rule.evaluate(facts);

        // Then
        assertThat(shouldFire).isTrue();

        // When
        try {
            rule.execute(facts);
        } catch (Exception e) {
            fail("Rule execution should not throw exception", e);
        }

        // Then
        assertThat((String) facts.get("riskLevel")).isEqualTo("HIGH");
        assertThat((Boolean) facts.get("requiresManualReview")).isEqualTo(true);
    }

    @Test
    public void testCreateRuleWithLowRiskProfession() {
        // Given
        Rule rule = ruleFactory.createRule("insurance_underwriting", "high_risk_profession_rule");
        Facts facts = new Facts();
        facts.put("applicant", new TestApplicant(30, "engineer"));

        // When
        boolean shouldFire = rule.evaluate(facts);

        // Then
        assertThat(shouldFire).isFalse();
    }

    @Test
    public void testCreateRuleWithNonExistentCatalog() {
        // When & Then
        assertThatThrownBy(() -> ruleFactory.createRule("non_existent_catalog", "some_rule"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Rule 'some_rule' not found in catalog 'non_existent_catalog'");
    }

    @Test
    public void testCreateRuleWithNonExistentRule() {
        // When & Then
        assertThatThrownBy(() -> ruleFactory.createRule("insurance_underwriting", "non_existent_rule"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Rule 'non_existent_rule' not found in catalog 'insurance_underwriting'");
    }

    @Test
    public void testCreateRulesFromEmptyCatalog() {
        // When
        Rules rules = ruleFactory.createRules("non_existent_catalog");

        // Then
        assertThat(rules).isNotNull();
        assertThat(rules.isEmpty()).isTrue();
    }

    @Test
    public void testNullParameterValidation() {
        // Test null catalog name in createRule
        assertThatThrownBy(() -> ruleFactory.createRule(null, "some_rule"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Catalog name must not be null");
            
        // Test null rule name in createRule
        assertThatThrownBy(() -> ruleFactory.createRule("insurance_underwriting", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Rule name must not be null");
            
        // Test null catalog name in createRules
        assertThatThrownBy(() -> ruleFactory.createRules(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Catalog name must not be null");
    }

    @Test
    public void testFraudDetectionRules() {
        // When
        Rules rules = ruleFactory.createRules("fraud_detection");

        // Then
        assertThat(rules).isNotNull();
        assertThat(rules.size()).isGreaterThan(0);
        
        // Test unusual transaction rule
        Rule unusualTransactionRule = findRuleByName(rules, "unusual_transaction_rule");
        assertThat(unusualTransactionRule).isNotNull();
        
        Facts facts = new Facts();
        facts.put("transaction", new TestTransaction(50000.0));
        facts.put("account", new TestAccount(5000.0));
        
        boolean shouldFire = unusualTransactionRule.evaluate(facts);
        assertThat(shouldFire).isTrue();
    }

    @Test
    public void testLoanApprovalRules() {
        // When
        Rules rules = ruleFactory.createRules("loan_approval");

        // Then
        assertThat(rules).isNotNull();
        assertThat(rules.size()).isGreaterThan(0);
        
        // Test income verification rule
        Rule incomeRule = findRuleByName(rules, "income_verification_rule");
        assertThat(incomeRule).isNotNull();
        
        Facts facts = new Facts();
        facts.put("applicant", new TestLoanApplicant(6000.0, 1500.0));
        facts.put("loan", new TestLoan(1500.0));
        
        boolean shouldFire = incomeRule.evaluate(facts);
        assertThat(shouldFire).isTrue();
    }

    private Rule findRuleByName(Rules rules, String name) {
        for (Rule rule : rules) {
            if (rule.getName().equals(name)) {
                return rule;
            }
        }
        return null;
    }

    // Test data classes
    public static class TestApplicant {
        public final int age;
        public final String profession;

        public TestApplicant(int age, String profession) {
            this.age = age;
            this.profession = profession;
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

        public TestAccount(double averageMonthlyTransactions) {
            this.averageMonthlyTransactions = averageMonthlyTransactions;
        }
    }

    public static class TestLoanApplicant {
        public final double monthlyIncome;
        public final double monthlyDebt;

        public TestLoanApplicant(double monthlyIncome, double monthlyDebt) {
            this.monthlyIncome = monthlyIncome;
            this.monthlyDebt = monthlyDebt;
        }
    }

    public static class TestLoan {
        public final double monthlyPayment;

        public TestLoan(double monthlyPayment) {
            this.monthlyPayment = monthlyPayment;
        }
    }
}