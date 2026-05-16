package com.clinic.server;

import com.clinic.server.entity.PatientEntity;
import com.clinic.server.entity.PrescriptionEntity;
import com.clinic.server.entity.UserEntity;
import com.clinic.server.repository.PatientRepository;
import com.clinic.server.repository.PrescriptionRepository;
import com.clinic.server.repository.UserRepository;
import com.clinic.shared.model.PatientStatus;
import com.clinic.shared.model.PrescriptionItem;
import com.clinic.shared.model.PrescriptionStatus;
import com.clinic.shared.model.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class ConfigDataLoader {
    private static final String ADMIN_ID = "admin-1";
    private static final String RECEPTIONIST_ID = "receptionist-1";
    private static final String DOCTOR_ID = "doctor-1";
    private static final String PHARMACIST_ID = "pharmacist-1";
    private static final String TESTING_STAFF_ID = "testing-staff-1";

    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               PatientRepository patientRepository,
                               PrescriptionRepository prescriptionRepository) {
        return args -> {
            seedUsers(userRepository);
            seedPatients(patientRepository);
            seedPrescriptions(prescriptionRepository, patientRepository);
        };
    }

    private void seedUsers(UserRepository userRepository) {
        if (userRepository.count() > 0) {
            return;
        }

        userRepository.save(makeUser(ADMIN_ID, "quantri", "123", Role.ADMIN, "Quản Trị Viên"));
        userRepository.save(makeUser(RECEPTIONIST_ID, "letan", "123", Role.RECEPTIONIST, "Lễ Tân Phòng Khám"));
        userRepository.save(makeUser(DOCTOR_ID, "bacsi", "123", Role.DOCTOR, "Bác Sĩ Nguyễn Văn Minh"));
        userRepository.save(makeUser(PHARMACIST_ID, "duocsi", "123", Role.PHARMACIST, "Dược Sĩ Trần Thị Lan"));
        userRepository.save(makeUser(TESTING_STAFF_ID, "xetnghiem", "123", Role.TESTING_STAFF, "Nhân Viên Xét Nghiệm"));
    }

    private void seedPatients(PatientRepository patientRepository) {
        if (patientRepository.count() > 0) {
            return;
        }

        patientRepository.save(makePatient("BN001", "Nguyễn Văn An", 30, "Nam", "0901111111", "Quận 1, TP. Hồ Chí Minh", DOCTOR_ID, PatientStatus.PRESCRIBED));
        patientRepository.save(makePatient("BN002", "Trần Thị Bình", 24, "Nữ", "0902222222", "Hải Châu, Đà Nẵng", DOCTOR_ID, PatientStatus.PRESCRIBED));
        patientRepository.save(makePatient("BN003", "Lê Văn Cường", 41, "Nam", "0903333333", "Cầu Giấy, Hà Nội", DOCTOR_ID, PatientStatus.COMPLETED));
        patientRepository.save(makePatient("BN004", "Phạm Thị Dung", 52, "Nữ", "0904444444", "Ninh Kiều, Cần Thơ", null, PatientStatus.WAITING));
    }

    private void seedPrescriptions(PrescriptionRepository prescriptionRepository, PatientRepository patientRepository) {
        if (prescriptionRepository.count() > 0) {
            return;
        }

        List<PatientEntity> patients = patientRepository.findAll();
        if (patients.isEmpty()) {
            return;
        }

        prescriptionRepository.save(makePrescription(patients.get(0).getId(), DOCTOR_ID, "Paracetamol", "2 viên/ngày", "Uống sau ăn", PrescriptionStatus.NEW));
        if (patients.size() > 1) {
            prescriptionRepository.save(makePrescription(patients.get(1).getId(), DOCTOR_ID, "Amoxicillin", "3 viên/ngày", "Uống sau bữa ăn", PrescriptionStatus.NEW));
        }
        if (patients.size() > 2) {
            prescriptionRepository.save(makePrescription(patients.get(2).getId(), DOCTOR_ID, "Vitamin C", "1 viên/ngày", "Uống vào buổi sáng", PrescriptionStatus.DISPENSED));
        }
    }

    private UserEntity makeUser(String id, String username, String password, Role role, String fullName) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setUsername(username);
        u.setPassword(password);
        u.setRole(role);
        u.setFullName(fullName);
        u.setActive(true);
        return u;
    }

    private PatientEntity makePatient(String code, String fullName, int age, String gender, String phone, String address, String assignedDoctorId, String status) {
        PatientEntity p = new PatientEntity();
        p.setCode(code);
        p.setFullName(fullName);
        p.setAge(age);
        p.setGender(gender);
        p.setPhone(phone);
        p.setAddress(address);
        p.setAssignedDoctorId(assignedDoctorId);
        p.setStatus(status);
        p.setLastVisitAt(LocalDateTime.now());
        return p;
    }

    private static int prescriptionSeedCounter = 1;

    private PrescriptionEntity makePrescription(String patientId, String doctorId, String medicineName, String dosage, String instructions, String status) {
        PrescriptionEntity p = new PrescriptionEntity();
        p.setCode(String.format("DT%03d", prescriptionSeedCounter++));
        p.setPatientId(patientId);
        p.setDoctorId(doctorId);
        p.setItems(List.of(new PrescriptionItem(medicineName, dosage, instructions)));
        p.setStatus(status);
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }
}
