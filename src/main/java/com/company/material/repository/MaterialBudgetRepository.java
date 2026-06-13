package com.company.material.repository;

import com.company.material.entity.MaterialBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MaterialBudgetRepository extends JpaRepository<MaterialBudget, Long> {

    List<MaterialBudget> findByFiscalYear(String fiscalYear);

    List<MaterialBudget> findByFiscalYearAndBudgetType(String fiscalYear, String budgetType);

    Optional<MaterialBudget> findByFiscalYearAndBudgetTypeAndTargetCode(String fiscalYear, String budgetType, String targetCode);

    void deleteByFiscalYearAndBudgetTypeAndTargetCode(String fiscalYear, String budgetType, String targetCode);
}
