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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that orchestrates policy catalog rule execution.
 * This service manages the loading, caching, and execution of rules from PostgreSQL database
 * organized by policy catalogs. It provides high-level operations for rule management
 * and execution with audit logging capabilities.
 *
 * @author Easy Rules Team
 */
public class PolicyCatalogOrchestratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyCatalogOrchestratorService.class);

    private final PostgreSQLRuleFactory ruleFactory;
    private final DefaultRulesEngine rulesEngine;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, Rules> catalogCache;
    private final boolean cacheEnabled;

    /**
     * Create a new PolicyCatalogOrchestratorService with caching enabled.
     *
     * @param dataSource the PostgreSQL data source
     */
    public PolicyCatalogOrchestratorService(DataSource dataSource) {
        this(dataSource, true);
    }

    /**
     * Create a new PolicyCatalogOrchestratorService.
     *
     * @param dataSource the PostgreSQL data source
     * @param cacheEnabled whether to enable catalog caching
     */
    public PolicyCatalogOrchestratorService(DataSource dataSource, boolean cacheEnabled) {
        this.ruleFactory = new PostgreSQLRuleFactory(dataSource);
        this.rulesEngine = new DefaultRulesEngine();
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.objectMapper = new ObjectMapper();
        this.catalogCache = new ConcurrentHashMap<>();
        this.cacheEnabled = cacheEnabled;
    }

    /**
     * Execute all rules in a policy catalog against the provided facts.
     *
     * @param catalogName the name of the policy catalog
     * @param facts the facts to evaluate rules against
     * @return execution summary with results
     */
    public RuleExecutionSummary executeRules(String catalogName, Facts facts) {
        Objects.requireNonNull(catalogName, "Catalog name must not be null");
        Objects.requireNonNull(facts, "Facts must not be null");

        long startTime = System.currentTimeMillis();
        RuleExecutionSummary summary = new RuleExecutionSummary(catalogName);

        try {
            Rules rules = loadRulesFromCatalog(catalogName);
            if (rules.isEmpty()) {
                LOGGER.warn("No active rules found in catalog: {}", catalogName);
                summary.setResult("NO_RULES");
                return summary;
            }

            LOGGER.info("Executing {} rules from catalog: {}", rules.size(), catalogName);
            
            // Execute rules and capture results
            Map<Rule, Boolean> ruleResults = rulesEngine.check(rules, facts);
            rulesEngine.fire(rules, facts);

            summary.setRuleResults(ruleResults);
            summary.setResult("SUCCESS");
            
            // Log execution details
            logRuleExecution(catalogName, facts, summary, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            LOGGER.error("Error executing rules from catalog: " + catalogName, e);
            summary.setResult("ERROR");
            summary.setErrorMessage(e.getMessage());
            
            // Log execution error
            logRuleExecutionError(catalogName, facts, e, System.currentTimeMillis() - startTime);
        }

        summary.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return summary;
    }

    /**
     * Execute a specific rule from a catalog against the provided facts.
     *
     * @param catalogName the name of the policy catalog
     * @param ruleName the name of the rule
     * @param facts the facts to evaluate the rule against
     * @return execution summary with results
     */
    public RuleExecutionSummary executeRule(String catalogName, String ruleName, Facts facts) {
        Objects.requireNonNull(catalogName, "Catalog name must not be null");
        Objects.requireNonNull(ruleName, "Rule name must not be null");
        Objects.requireNonNull(facts, "Facts must not be null");

        long startTime = System.currentTimeMillis();
        RuleExecutionSummary summary = new RuleExecutionSummary(catalogName);

        try {
            Rule rule = ruleFactory.createRule(catalogName, ruleName);
            Rules rules = new Rules();
            rules.register(rule);

            LOGGER.info("Executing rule '{}' from catalog: {}", ruleName, catalogName);
            
            // Execute rule and capture results
            Map<Rule, Boolean> ruleResults = rulesEngine.check(rules, facts);
            rulesEngine.fire(rules, facts);

            summary.setRuleResults(ruleResults);
            summary.setResult("SUCCESS");
            
            // Log execution details
            logRuleExecution(catalogName, facts, summary, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            LOGGER.error("Error executing rule '" + ruleName + "' from catalog: " + catalogName, e);
            summary.setResult("ERROR");
            summary.setErrorMessage(e.getMessage());
            
            // Log execution error
            logRuleExecutionError(catalogName, facts, e, System.currentTimeMillis() - startTime);
        }

        summary.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return summary;
    }

    /**
     * Get all available policy catalog names.
     *
     * @return list of active catalog names
     */
    public List<String> getAvailableCatalogs() {
        Rules allRules = ruleFactory.createAllRules();
        Set<String> catalogNames = new HashSet<>();
        for (Rule rule : allRules) {
            catalogNames.add(rule.getName());
        }
        return new ArrayList<>(catalogNames);
    }

    /**
     * Refresh the cache for a specific catalog.
     *
     * @param catalogName the name of the catalog to refresh
     */
    public void refreshCatalogCache(String catalogName) {
        if (cacheEnabled) {
            catalogCache.remove(catalogName);
            LOGGER.info("Cache refreshed for catalog: {}", catalogName);
        }
    }

    /**
     * Clear all cached catalogs.
     */
    public void clearCache() {
        if (cacheEnabled) {
            catalogCache.clear();
            LOGGER.info("All catalog cache cleared");
        }
    }

    /**
     * Check if a catalog exists and is active.
     *
     * @param catalogName the name of the catalog
     * @return true if catalog exists and is active
     */
    public boolean catalogExists(String catalogName) {
        String sql = "SELECT COUNT(*) FROM policy_catalog WHERE name = ? AND status = 'ACTIVE'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, catalogName);
        return count != null && count > 0;
    }

    private Rules loadRulesFromCatalog(String catalogName) {
        if (cacheEnabled && catalogCache.containsKey(catalogName)) {
            LOGGER.debug("Loading rules from cache for catalog: {}", catalogName);
            return catalogCache.get(catalogName);
        }

        LOGGER.debug("Loading rules from database for catalog: {}", catalogName);
        Rules rules = ruleFactory.createRules(catalogName);
        
        if (cacheEnabled) {
            catalogCache.put(catalogName, rules);
        }

        return rules;
    }

    private void logRuleExecution(String catalogName, Facts facts, RuleExecutionSummary summary, long executionTimeMs) {
        try {
            String factsJson = objectMapper.writeValueAsString(facts.asMap());
            String sql = "INSERT INTO rule_execution_log (catalog_id, execution_context, execution_result, execution_time_ms) " +
                        "SELECT pc.id, ?::jsonb, ?, ? " +
                        "FROM policy_catalog pc " +
                        "WHERE pc.name = ?";
            
            jdbcTemplate.update(sql, factsJson, summary.getResult(), executionTimeMs, catalogName);
            
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to serialize facts for audit log", e);
        } catch (Exception e) {
            LOGGER.warn("Failed to log rule execution", e);
        }
    }

    private void logRuleExecutionError(String catalogName, Facts facts, Exception error, long executionTimeMs) {
        try {
            String factsJson = objectMapper.writeValueAsString(facts.asMap());
            String sql = "INSERT INTO rule_execution_log (catalog_id, execution_context, execution_result, execution_time_ms, error_message) " +
                        "SELECT pc.id, ?::jsonb, 'ERROR', ?, ? " +
                        "FROM policy_catalog pc " +
                        "WHERE pc.name = ?";
            
            jdbcTemplate.update(sql, factsJson, executionTimeMs, error.getMessage(), catalogName);
            
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to serialize facts for error audit log", e);
        } catch (Exception e) {
            LOGGER.warn("Failed to log rule execution error", e);
        }
    }
}