package com.placementos.backend.controller;

import com.placementos.backend.domain.dto.StudentRequest;
import com.placementos.backend.domain.dto.StudentResponse;
import com.placementos.backend.domain.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Student operations.
 * Handles HTTP concerns: mapping, validation, status codes, and routing to the service layer.
 */
@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody StudentRequest request) {
        StudentResponse response = studentService.createStudent(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable Long id) {
        StudentResponse response = studentService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/registration/{registrationNumber}")
    public ResponseEntity<StudentResponse> getStudentByRegistrationNumber(@PathVariable String registrationNumber) {
        return studentService.findByRegistrationNumber(registrationNumber)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/neopat/{neopatId}")
    public ResponseEntity<StudentResponse> getStudentByNeopatId(@PathVariable String neopatId) {
        return studentService.findByNeopatId(neopatId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequest request) {
        StudentResponse response = studentService.updateStudent(id, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAllStudents() {
        List<StudentResponse> students = studentService.findAll();
        return ResponseEntity.ok(students);
    }
}
