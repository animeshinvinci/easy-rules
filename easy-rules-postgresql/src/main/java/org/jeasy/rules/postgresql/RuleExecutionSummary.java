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

import java.util.HashMap;
import java.util.Map;

/**
 * Summary of rule execution results.
 * Contains information about which rules were triggered, execution time,
 * and any errors that occurred during execution.
 *
 * @author Easy Rules Team
 */
public class RuleExecutionSummary {

    private final String catalogName;
    private Map<Rule, Boolean> ruleResults;
    private String result;
    private String errorMessage;
    private long executionTimeMs;

    public RuleExecutionSummary(String catalogName) {
        this.catalogName = catalogName;
        this.ruleResults = new HashMap<>();
    }

    public String getCatalogName() {
        return catalogName;
    }

    public Map<Rule, Boolean> getRuleResults() {
        return ruleResults;
    }

    public void setRuleResults(Map<Rule, Boolean> ruleResults) {
        this.ruleResults = ruleResults;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    /**
     * Get the number of rules that were triggered (evaluated to true).
     *
     * @return count of triggered rules
     */
    public long getTriggeredRulesCount() {
        return ruleResults.values().stream()
                .mapToLong(triggered -> triggered ? 1 : 0)
                .sum();
    }

    /**
     * Get the total number of rules evaluated.
     *
     * @return total count of rules
     */
    public int getTotalRulesCount() {
        return ruleResults.size();
    }

    /**
     * Check if the execution was successful.
     *
     * @return true if execution was successful
     */
    public boolean isSuccessful() {
        return "SUCCESS".equals(result);
    }

    @Override
    public String toString() {
        return "RuleExecutionSummary{" +
                "catalogName='" + catalogName + '\'' +
                ", totalRules=" + getTotalRulesCount() +
                ", triggeredRules=" + getTriggeredRulesCount() +
                ", result='" + result + '\'' +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}