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

import org.jeasy.rules.support.RuleDefinition;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads rule definitions from PostgreSQL database.
 *
 * @author Easy Rules Team
 */
public class PostgreSQLRuleDefinitionReader {

    private final JdbcTemplate jdbcTemplate;

    public PostgreSQLRuleDefinitionReader(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * Read all rule definitions from a policy catalog.
     *
     * @param catalogName the name of the policy catalog
     * @return list of rule definitions
     */
    public List<RuleDefinition> readRuleDefinitions(String catalogName) {
        String sql = "SELECT rd.id, rd.name, rd.description, rd.priority, rd.rule_type, " +
                    "rd.composite_rule_type, rd.condition_expression " +
                    "FROM rule_definitions rd " +
                    "JOIN policy_catalog pc ON rd.catalog_id = pc.id " +
                    "WHERE pc.name = ? AND pc.status = 'ACTIVE' AND rd.status = 'ACTIVE' " +
                    "ORDER BY rd.priority ASC";

        List<RuleDefinition> ruleDefinitions = jdbcTemplate.query(sql, new RuleDefinitionRowMapper(), catalogName);
        
        // Load actions for each rule
        for (RuleDefinition ruleDefinition : ruleDefinitions) {
            loadActionsForRule(ruleDefinition);
            if ("COMPOSITE".equals(ruleDefinition.getCompositeRuleType())) {
                loadComposingRules(ruleDefinition);
            }
        }

        return ruleDefinitions;
    }

    /**
     * Read a specific rule definition by catalog and rule name.
     *
     * @param catalogName the name of the policy catalog
     * @param ruleName the name of the rule
     * @return rule definition or null if not found
     */
    public RuleDefinition readRuleDefinition(String catalogName, String ruleName) {
        String sql = "SELECT rd.id, rd.name, rd.description, rd.priority, rd.rule_type, " +
                    "rd.composite_rule_type, rd.condition_expression " +
                    "FROM rule_definitions rd " +
                    "JOIN policy_catalog pc ON rd.catalog_id = pc.id " +
                    "WHERE pc.name = ? AND rd.name = ? AND pc.status = 'ACTIVE' AND rd.status = 'ACTIVE'";

        List<RuleDefinition> results = jdbcTemplate.query(sql, new RuleDefinitionRowMapper(), catalogName, ruleName);
        if (results.isEmpty()) {
            return null;
        }

        RuleDefinition ruleDefinition = results.get(0);
        loadActionsForRule(ruleDefinition);
        if ("COMPOSITE".equals(ruleDefinition.getCompositeRuleType())) {
            loadComposingRules(ruleDefinition);
        }

        return ruleDefinition;
    }

    private void loadActionsForRule(RuleDefinition ruleDefinition) {
        String sql = "SELECT action_expression " +
                    "FROM rule_actions " +
                    "WHERE rule_id = ? " +
                    "ORDER BY action_order ASC";

        List<String> actions = jdbcTemplate.queryForList(sql, String.class, getRuleId(ruleDefinition));
        ruleDefinition.setActions(actions);
    }

    private void loadComposingRules(RuleDefinition parentRule) {
        String sql = "SELECT rd.id, rd.name, rd.description, rd.priority, rd.rule_type, " +
                    "rd.composite_rule_type, rd.condition_expression " +
                    "FROM rule_definitions rd " +
                    "JOIN rule_compositions rc ON rd.id = rc.child_rule_id " +
                    "WHERE rc.parent_rule_id = ? " +
                    "ORDER BY rc.rule_order ASC";

        List<RuleDefinition> composingRules = jdbcTemplate.query(sql, new RuleDefinitionRowMapper(), getRuleId(parentRule));
        
        // Recursively load actions and composing rules for each child rule
        for (RuleDefinition composingRule : composingRules) {
            loadActionsForRule(composingRule);
            if ("COMPOSITE".equals(composingRule.getCompositeRuleType())) {
                loadComposingRules(composingRule);
            }
        }

        parentRule.setComposingRules(composingRules);
    }

    private Long getRuleId(RuleDefinition ruleDefinition) {
        // Store rule ID in a custom property for internal use
        if (ruleDefinition instanceof ExtendedRuleDefinition) {
            return ((ExtendedRuleDefinition) ruleDefinition).getRuleId();
        }
        return null;
    }

    /**
     * Get all active catalog names.
     *
     * @return list of active catalog names
     */
    public List<String> getActiveCatalogNames() {
        String sql = "SELECT name FROM policy_catalog WHERE status = 'ACTIVE' ORDER BY name";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    private static class RuleDefinitionRowMapper implements RowMapper<RuleDefinition> {
        @Override
        public RuleDefinition mapRow(ResultSet rs, int rowNum) throws SQLException {
            ExtendedRuleDefinition ruleDefinition = new ExtendedRuleDefinition();
            ruleDefinition.setRuleId(rs.getLong("id"));
            ruleDefinition.setName(rs.getString("name"));
            ruleDefinition.setDescription(rs.getString("description"));
            ruleDefinition.setPriority(rs.getInt("priority"));
            ruleDefinition.setCondition(rs.getString("condition_expression"));
            
            String ruleType = rs.getString("rule_type");
            if ("COMPOSITE".equals(ruleType)) {
                ruleDefinition.setCompositeRuleType(rs.getString("composite_rule_type"));
            }

            return ruleDefinition;
        }
    }
}