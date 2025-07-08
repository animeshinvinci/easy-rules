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

import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.mvel.MVELRule;
import org.jeasy.rules.support.AbstractRuleFactory;
import org.jeasy.rules.support.RuleDefinition;
import org.mvel2.ParserContext;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;

/**
 * Factory to create {@link MVELRule} instances from PostgreSQL database.
 * This factory reads rule definitions from a PostgreSQL database and creates
 * MVEL-based rules that can be executed by the Easy Rules engine.
 *
 * @author Easy Rules Team
 */
public class PostgreSQLRuleFactory extends AbstractRuleFactory {

    private final PostgreSQLRuleDefinitionReader reader;
    private final ParserContext parserContext;

    /**
     * Create a new {@link PostgreSQLRuleFactory} with a given data source.
     *
     * @param dataSource the PostgreSQL data source
     */
    public PostgreSQLRuleFactory(DataSource dataSource) {
        this(dataSource, new ParserContext());
    }

    /**
     * Create a new {@link PostgreSQLRuleFactory} with a given data source and parser context.
     *
     * @param dataSource the PostgreSQL data source
     * @param parserContext used to parse condition/action expressions
     */
    public PostgreSQLRuleFactory(DataSource dataSource, ParserContext parserContext) {
        this.reader = new PostgreSQLRuleDefinitionReader(dataSource);
        this.parserContext = parserContext;
    }

    /**
     * Create a rule from database by catalog name and rule name.
     *
     * @param catalogName the name of the policy catalog
     * @param ruleName the name of the rule
     * @return a new rule instance
     * @throws IllegalArgumentException if rule is not found
     */
    public Rule createRule(String catalogName, String ruleName) {
        Objects.requireNonNull(catalogName, "Catalog name must not be null");
        Objects.requireNonNull(ruleName, "Rule name must not be null");

        RuleDefinition ruleDefinition = reader.readRuleDefinition(catalogName, ruleName);
        if (ruleDefinition == null) {
            throw new IllegalArgumentException("Rule '" + ruleName + "' not found in catalog '" + catalogName + "'");
        }

        return createRule(ruleDefinition);
    }

    /**
     * Create all rules from a policy catalog.
     *
     * @param catalogName the name of the policy catalog
     * @return a set of rules from the catalog
     */
    public Rules createRules(String catalogName) {
        Objects.requireNonNull(catalogName, "Catalog name must not be null");

        Rules rules = new Rules();
        List<RuleDefinition> ruleDefinitions = reader.readRuleDefinitions(catalogName);
        
        for (RuleDefinition ruleDefinition : ruleDefinitions) {
            rules.register(createRule(ruleDefinition));
        }

        return rules;
    }

    /**
     * Create all active rules from all active catalogs.
     *
     * @return a set of all active rules
     */
    public Rules createAllRules() {
        Rules rules = new Rules();
        
        // Get all active catalog names and create rules for each
        List<String> catalogNames = reader.getActiveCatalogNames();
        for (String catalogName : catalogNames) {
            Rules catalogRules = createRules(catalogName);
            catalogRules.forEach(rules::register);
        }

        return rules;
    }

    @Override
    protected Rule createSimpleRule(RuleDefinition ruleDefinition) {
        MVELRule mvelRule = new MVELRule(parserContext)
                .name(ruleDefinition.getName())
                .description(ruleDefinition.getDescription())
                .priority(ruleDefinition.getPriority());

        // Set condition if present
        if (ruleDefinition.getCondition() != null && !ruleDefinition.getCondition().trim().isEmpty()) {
            mvelRule.when(ruleDefinition.getCondition());
        }

        // Add actions
        for (String action : ruleDefinition.getActions()) {
            mvelRule.then(action);
        }

        return mvelRule;
    }
}