package com.stmanagement.repository;

import com.stmanagement.model.EmployeeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeAttachmentRepository extends JpaRepository<EmployeeAttachment, Long> {

    List<EmployeeAttachment> findByEmployeeId(Long employeeId);

    void deleteByEmployeeId(Long employeeId);
}
