package com.odontoflow.controller;

import com.odontoflow.entity.Patient;
import com.odontoflow.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    public ResponseEntity<List<Patient>> findAll (){
        return ResponseEntity.ok(patientService.findAllActive());
    }

    @PostMapping
    public ResponseEntity<Patient> create(@RequestBody Patient patient){
        Patient savedPatient = patientService.save(patient);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPatient);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id){
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }


}
