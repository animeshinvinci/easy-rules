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

/**
 * Insurance application data model.
 * Represents an insurance application with applicant details.
 *
 * @author Easy Rules Team
 */
public class InsuranceApplication {

    private final Applicant applicant;
    private String status;
    private String riskLevel;
    private boolean requiresManualReview;
    private String rejectionReason;

    public InsuranceApplication(Applicant applicant) {
        this.applicant = applicant;
        this.status = "PENDING";
        this.riskLevel = "UNKNOWN";
        this.requiresManualReview = false;
    }

    public Applicant getApplicant() {
        return applicant;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public boolean isRequiresManualReview() {
        return requiresManualReview;
    }

    public void setRequiresManualReview(boolean requiresManualReview) {
        this.requiresManualReview = requiresManualReview;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    @Override
    public String toString() {
        return "InsuranceApplication{" +
                "applicant=" + applicant +
                ", status='" + status + '\'' +
                ", riskLevel='" + riskLevel + '\'' +
                ", requiresManualReview=" + requiresManualReview +
                ", rejectionReason='" + rejectionReason + '\'' +
                '}';
    }

    /**
     * Applicant data model.
     */
    public static class Applicant {
        public final int age;
        public final String profession;
        public final int creditScore;
        public final String name;

        public Applicant(String name, int age, String profession, int creditScore) {
            this.name = name;
            this.age = age;
            this.profession = profession;
            this.creditScore = creditScore;
        }

        @Override
        public String toString() {
            return "Applicant{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    ", profession='" + profession + '\'' +
                    ", creditScore=" + creditScore +
                    '}';
        }
    }
}