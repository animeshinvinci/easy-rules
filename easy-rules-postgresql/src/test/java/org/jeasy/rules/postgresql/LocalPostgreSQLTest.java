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
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Local PostgreSQL test that doesn't require Docker/Testcontainers.
 * Requires a local PostgreSQL instance with the easyrules_test database.
 */
public class LocalPostgreSQLTest {

    private DataSource dataSource;
    private PostgreSQLRuleFactory ruleFactory;

    @Before
    public void setUp() {
        // Configure HikariCP data source for local PostgreSQL
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/easyrules_test");
        config.setUsername("animeshmishra");
        config.setPassword("");
        config.setMaximumPoolSize(5);
        
        dataSource = new HikariDataSource(config);

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
    public void testRuleExecution() {
        // Given
        Rule rule = ruleFactory.createRule("insurance_underwriting", "age_validation_rule");
        Facts facts = new Facts();
        TestApplicant applicant = new TestApplicant(25, "engineer");
        facts.put("applicant", applicant);
        
        // Create a result holder that MVEL can modify
        ResultHolder result = new ResultHolder();
        facts.put("result", result);

        // When
        boolean shouldFire = rule.evaluate(facts);

        // Then
        assertThat(shouldFire).isTrue();

        // When
        try {
            rule.execute(facts);
            // Success - no exception thrown
        } catch (Exception e) {
            // Print the exception for debugging
            e.printStackTrace();
            fail("Rule execution threw exception: " + e.getMessage());
        }
        
        // For now, just verify that the rule executed without errors
        // The actual validation would depend on how the action is written in the database
    }

    // Test data class
    public static class TestApplicant {
        public final int age;
        public final String profession;

        public TestApplicant(int age, String profession) {
            this.age = age;
            this.profession = profession;
        }
    }
    
    // Result holder for MVEL to modify
    public static class ResultHolder {
        private String ageValidationResult;
        
        public String getAgeValidationResult() {
            return ageValidationResult;
        }
        
        public void setAgeValidationResult(String result) {
            this.ageValidationResult = result;
        }
    }
}