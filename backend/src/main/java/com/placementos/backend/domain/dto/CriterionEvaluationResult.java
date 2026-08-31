package com.placementos.backend.domain.dto;

import com.placementos.backend.domain.enums.CriterionEvaluationStatus;
import com.placementos.backend.domain.enums.CriterionScope;
import com.placementos.backend.domain.enums.CriterionType;

/**
 * Detailed explainable result of evaluating a single criterion.
 */
public class CriterionEvaluationResult {

    private CriterionType criterion;
    private CriterionEvaluationStatus status;
    private CriterionScope scope;
    private Object requiredValue;
    private Object actualValue;
    private String reason;

    public CriterionEvaluationResult() {}

    public CriterionEvaluationResult(CriterionType criterion,
                                     CriterionEvaluationStatus status,
                                     CriterionScope scope,
                                     Object requiredValue,
                                     Object actualValue,
                                     String reason) {
        this.criterion = criterion;
        this.status = status;
        this.scope = scope;
        this.requiredValue = requiredValue;
        this.actualValue = actualValue;
        this.reason = reason;
    }

    public CriterionType getCriterion() { return criterion; }
    public void setCriterion(CriterionType criterion) { this.criterion = criterion; }

    public CriterionEvaluationStatus getStatus() { return status; }
    public void setStatus(CriterionEvaluationStatus status) { this.status = status; }

    public CriterionScope getScope() { return scope; }
    public void setScope(CriterionScope scope) { this.scope = scope; }

    public Object getRequiredValue() { return requiredValue; }
    public void setRequiredValue(Object requiredValue) { this.requiredValue = requiredValue; }

    public Object getActualValue() { return actualValue; }
    public void setActualValue(Object actualValue) { this.actualValue = actualValue; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
