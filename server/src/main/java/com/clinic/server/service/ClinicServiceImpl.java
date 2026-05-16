package com.clinic.server.service;

import com.clinic.server.entity.MedicalRecordEntity;
import com.clinic.server.entity.PatientEntity;
import com.clinic.server.entity.PrescriptionEntity;
import com.clinic.server.entity.TestResultEntity;
import com.clinic.server.entity.UserEntity;
import com.clinic.server.repository.MedicalRecordRepository;
import com.clinic.server.repository.PatientRepository;
import com.clinic.server.repository.PrescriptionRepository;
import com.clinic.server.repository.TestResultRepository;
import com.clinic.server.repository.UserRepository;
import com.clinic.server.websocket.RealtimeEventPublisher;
import com.clinic.shared.dto.ApiResponse;
import com.clinic.shared.dto.AssignDoctorRequest;
import com.clinic.shared.dto.ChangePasswordRequest;
import com.clinic.shared.dto.CreatePrescriptionRequest;
import com.clinic.shared.dto.DispensePrescriptionRequest;
import com.clinic.shared.dto.LoginRequest;
import com.clinic.shared.dto.LoginResponse;
import com.clinic.shared.dto.MedicalRecordRequest;
import com.clinic.shared.dto.PrescriptionSearchRequest;
import com.clinic.shared.dto.PrescriptionUpdateRequest;
import com.clinic.shared.dto.RealtimeEventType;
import com.clinic.shared.dto.ReceptionIntakeRequest;
import com.clinic.shared.dto.SearchPatientRequest;
import com.clinic.shared.dto.TestResultRequest;
import com.clinic.shared.dto.UserRequest;
import com.clinic.shared.dto.UserSearchRequest;
import com.clinic.shared.model.MedicalRecord;
import com.clinic.shared.model.Patient;
import com.clinic.shared.model.PatientStatus;
import com.clinic.shared.model.Prescription;
import com.clinic.shared.model.PrescriptionStatus;
import com.clinic.shared.model.Role;
import com.clinic.shared.model.TestResult;
import com.clinic.shared.model.User;
import com.clinic.shared.rmi.ClinicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClinicServiceImpl extends UnicastRemoteObject implements ClinicService {
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final TestResultRepository testResultRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @Autowired
    public ClinicServiceImpl(UserRepository userRepository, PatientRepository patientRepository,
                             MedicalRecordRepository medicalRecordRepository, PrescriptionRepository prescriptionRepository,
                             TestResultRepository testResultRepository,
                             RealtimeEventPublisher realtimeEventPublisher) throws RemoteException {
        super();
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.testResultRepository = testResultRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        return userRepository.findByUsernameAndPassword(trim(request.getUsername()), request.getPassword())
                .map(u -> u.isActive()
                        ? new LoginResponse(true, "Đăng nhập thành công", UserMapper.toModel(u))
                        : new LoginResponse(false, "Tài khoản đã bị khóa", null))
                .orElse(new LoginResponse(false, "Sai tài khoản hoặc mật khẩu", null));
    }

    @Override
    public User getMyProfile(String userId) {
        return userRepository.findById(userId).map(UserMapper::toModel).orElse(null);
    }

    @Override
    public ApiResponse changePassword(ChangePasswordRequest request) {
        return userRepository.findById(request.getUserId()).map(user -> {
            if (!user.getPassword().equals(request.getCurrentPassword())) {
                return new ApiResponse(false, "Mật khẩu hiện tại không đúng");
            }
            user.setPassword(request.getNewPassword());
            userRepository.save(user);
            publishRealtimeEvent(RealtimeEventType.USER_CHANGED, user.getId(), "Mật khẩu người dùng đã được thay đổi");
            return new ApiResponse(true, "Đổi mật khẩu thành công");
        }).orElse(new ApiResponse(false, "Không tìm thấy người dùng"));
    }

    @Override
    public ApiResponse receivePatient(ReceptionIntakeRequest request) {
        if (isBlank(request.getCode()) || isBlank(request.getFullName())) {
            return new ApiResponse(false, "Mã bệnh nhân và họ tên không được để trống");
        }
        if (!isBlank(request.getDoctorId())) {
            ApiResponse doctorCheck = validateActiveUserRole(request.getDoctorId(), Role.DOCTOR, "bác sĩ");
            if (!doctorCheck.isSuccess()) {
                return doctorCheck;
            }
        }

        PatientEntity existing = patientRepository.findByCode(trim(request.getCode())).orElse(null);
        boolean isNew = existing == null;
        PatientEntity patient = isNew ? new PatientEntity() : existing;

        if (isNew) {
            patient.setId(UUID.randomUUID().toString());
            patient.setCode(trim(request.getCode()));
            patient.setFullName(trim(request.getFullName()));
            patient.setAge(request.getAge());
            patient.setGender(trim(request.getGender()));
            patient.setPhone(trim(request.getPhone()));
            patient.setAddress(trim(request.getAddress()));
        }

        patient.setAssignedDoctorId(trim(request.getDoctorId()));
        patient.setStatus(isBlank(request.getDoctorId()) ? PatientStatus.WAITING : PatientStatus.ASSIGNED);
        patient.setLastVisitAt(LocalDateTime.now());
        patientRepository.save(patient);
        publishRealtimeEvent(RealtimeEventType.PATIENT_CHANGED, patient.getId(), "Bệnh nhân đã được tiếp nhận");
        return new ApiResponse(true, isNew ? "Đã tiếp nhận bệnh nhân mới" : "Đã tiếp nhận bệnh nhân cũ vào hàng chờ khám");
    }

    @Override
    public ApiResponse addPatient(Patient patient) {
        if (patient == null || isBlank(patient.getCode()) || isBlank(patient.getFullName())) {
            return new ApiResponse(false, "Mã bệnh nhân và họ tên không được để trống");
        }
        if (patientRepository.findByCode(trim(patient.getCode())).isPresent()) {
            return new ApiResponse(false, "Mã bệnh nhân đã tồn tại");
        }
        if (!isBlank(patient.getAssignedDoctorId())) {
            ApiResponse doctorCheck = validateActiveUserRole(patient.getAssignedDoctorId(), Role.DOCTOR, "bác sĩ");
            if (!doctorCheck.isSuccess()) {
                return doctorCheck;
            }
        }

        PatientEntity e = new PatientEntity();
        e.setId(UUID.randomUUID().toString());
        e.setCode(trim(patient.getCode()));
        e.setFullName(trim(patient.getFullName()));
        e.setAge(patient.getAge());
        e.setGender(trim(patient.getGender()));
        e.setPhone(trim(patient.getPhone()));
        e.setAddress(trim(patient.getAddress()));
        e.setAssignedDoctorId(trim(patient.getAssignedDoctorId()));
        e.setStatus(patient.getStatus() == null ? PatientStatus.WAITING : patient.getStatus());
        patientRepository.save(e);
        publishRealtimeEvent(RealtimeEventType.PATIENT_CHANGED, e.getId(), "Bệnh nhân mới đã được thêm");
        return new ApiResponse(true, "Đã thêm bệnh nhân thành công");
    }

    @Override
    public ApiResponse updatePatient(Patient patient) {
        if (patient == null || isBlank(patient.getId()) || isBlank(patient.getCode()) || isBlank(patient.getFullName())) {
            return new ApiResponse(false, "Thiếu thông tin bệnh nhân cần cập nhật");
        }
        return patientRepository.findById(patient.getId()).map(e -> {
            PatientEntity duplicated = patientRepository.findByCode(trim(patient.getCode())).orElse(null);
            if (duplicated != null && !duplicated.getId().equals(patient.getId())) {
                return new ApiResponse(false, "Mã bệnh nhân đã tồn tại");
            }
            if (!isBlank(patient.getAssignedDoctorId())) {
                ApiResponse doctorCheck = validateActiveUserRole(patient.getAssignedDoctorId(), Role.DOCTOR, "bác sĩ");
                if (!doctorCheck.isSuccess()) {
                    return doctorCheck;
                }
            }
            e.setCode(trim(patient.getCode()));
            e.setFullName(trim(patient.getFullName()));
            e.setAge(patient.getAge());
            e.setGender(trim(patient.getGender()));
            e.setPhone(trim(patient.getPhone()));
            e.setAddress(trim(patient.getAddress()));
            e.setAssignedDoctorId(trim(patient.getAssignedDoctorId()));
            e.setStatus(patient.getStatus());
            e.setLastVisitAt(patient.getLastVisitAt());
            patientRepository.save(e);
            publishRealtimeEvent(RealtimeEventType.PATIENT_CHANGED, e.getId(), "Thông tin bệnh nhân đã được cập nhật");
            return new ApiResponse(true, "Đã cập nhật bệnh nhân thành công");
        }).orElse(new ApiResponse(false, "Không tìm thấy bệnh nhân"));
    }

    @Override
    public ApiResponse deletePatient(String patientCode) {
        PatientEntity patient = patientRepository.findByCode(patientCode).orElse(null);
        if (patient == null) {
            return new ApiResponse(false, "Không tìm thấy bệnh nhân");
        }
        if (!medicalRecordRepository.findByPatientId(patient.getId()).isEmpty()) {
            return new ApiResponse(false, "Không thể xóa bệnh nhân vì còn hồ sơ bệnh án liên quan");
        }
        if (!prescriptionRepository.findByPatientId(patient.getId()).isEmpty()) {
            return new ApiResponse(false, "Không thể xóa bệnh nhân vì còn toa thuốc liên quan");
        }
        if (!testResultRepository.findByPatientId(patient.getId()).isEmpty()) {
            return new ApiResponse(false, "Không thể xóa bệnh nhân vì còn kết quả xét nghiệm liên quan");
        }
        if (patient.getLastVisitAt() == null) {
            return new ApiResponse(false, "Không thể xóa vì chưa có thông tin lần khám gần nhất");
        }
        if (patient.getLastVisitAt().isAfter(LocalDateTime.now().minusYears(3))) {
            return new ApiResponse(false, "Chỉ được xóa bệnh nhân không tái khám quá 3 năm");
        }
        patientRepository.delete(patient);
        publishRealtimeEvent(RealtimeEventType.PATIENT_CHANGED, patient.getId(), "Bệnh nhân đã được xóa");
        return new ApiResponse(true, "Đã xóa bệnh nhân đủ điều kiện");
    }

    @Override
    public List<Patient> getAllPatients() {
        return patientRepository.findAll().stream().map(Mapper::toModel).collect(Collectors.toList());
    }

    @Override
    public List<Patient> getPatientsByDoctor(String doctorId) {
        return patientRepository.findAll().stream()
                .map(Mapper::toModel)
                .filter(p -> doctorId.equals(p.getAssignedDoctorId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Patient> getWaitingPatientsByDoctor(String doctorId) {
        return patientRepository.findAll().stream()
                .map(Mapper::toModel)
                .filter(p -> doctorId.equals(p.getAssignedDoctorId()))
                .filter(p -> p.getStatus() != null && (p.getStatus().equalsIgnoreCase(PatientStatus.WAITING) || p.getStatus().equalsIgnoreCase(PatientStatus.ASSIGNED)))
                .collect(Collectors.toList());
    }

    @Override
    public ApiResponse acceptPatient(String patientCode, String doctorId) {
        PatientEntity patient = patientRepository.findByCode(patientCode).orElseGet(() -> patientRepository.findById(patientCode).orElse(null));
        if (patient == null) {
            return new ApiResponse(false, "Không tìm thấy bệnh nhân");
        }
        if (!doctorId.equals(patient.getAssignedDoctorId())) {
            return new ApiResponse(false, "Bệnh nhân không thuộc danh sách chờ của bác sĩ này");
        }
        if (patient.getStatus() == null || !(patient.getStatus().equalsIgnoreCase(PatientStatus.WAITING)
                || patient.getStatus().equalsIgnoreCase(PatientStatus.ASSIGNED))) {
            return new ApiResponse(false, "Chỉ được nhận bệnh nhân đang chờ xử lý hoặc đã phân công");
        }
        patient.setStatus(PatientStatus.EXAMINING);
        patientRepository.save(patient);
        publishRealtimeEvent(RealtimeEventType.PATIENT_CHANGED, patient.getId(), "Bác sĩ đã nhận bệnh nhân vào khám");
        return new ApiResponse(true, "Đã nhận bệnh nhân vào khám");
    }

    @Override
    public List<Patient> searchPatients(SearchPatientRequest request) {
        String keyword = request.getKeyword() == null ? "" : request.getKeyword().toLowerCase();
        return patientRepository.findAll().stream()
                .map(Mapper::toModel)
                .filter(p -> p.getCode().toLowerCase().contains(keyword)
                        || p.getFullName().toLowerCase().contains(keyword)
                        || (p.getPhone() != null && p.getPhone().contains(keyword))
                        || (p.getStatus() != null && p.getStatus().toLowerCase().contains(keyword)))
                .collect(Collectors.toList());
    }

    @Override
    public ApiResponse assignDoctor(AssignDoctorRequest request) {
        PatientEntity target = patientRepository.findByCode(request.getPatientId()).orElseGet(() -> patientRepository.findById(request.getPatientId()).orElse(null));
        if (target == null) {
            return new ApiResponse(false, "Không tìm thấy bệnh nhân");
        }
        ApiResponse doctorCheck = validateActiveUserRole(request.getDoctorId(), Role.DOCTOR, "bác sĩ");
        if (!doctorCheck.isSuccess()) {
            return doctorCheck;
        }
        target.setAssignedDoctorId(request.getDoctorId());
        target.setStatus(PatientStatus.ASSIGNED);
        patientRepository.save(target);
        publishRealtimeEvent(RealtimeEventType.PATIENT_CHANGED, target.getId(), "Bệnh nhân đã được phân công bác sĩ");
        return new ApiResponse(true, "Đã phân công bác sĩ");
    }

    @Override
    public ApiResponse createMedicalRecord(MedicalRecordRequest request) {
        PatientEntity patient = patientRepository.findByCode(request.getPatientId()).orElseGet(() -> patientRepository.findById(request.getPatientId()).orElse(null));
        if (patient == null) {
            return new ApiResponse(false, "Không tìm thấy bệnh nhân");
        }
        ApiResponse doctorCheck = validateActiveUserRole(request.getDoctorId(), Role.DOCTOR, "bác sĩ");
        if (!doctorCheck.isSuccess()) {
            return doctorCheck;
        }
        if (!request.getDoctorId().equals(patient.getAssignedDoctorId())) {
            return new ApiResponse(false, "Bệnh nhân không thuộc bác sĩ này");
        }
        if (patient.getStatus() == null || !(patient.getStatus().equalsIgnoreCase(PatientStatus.EXAMINING)
                || patient.getStatus().equalsIgnoreCase(PatientStatus.HAS_TEST_RESULT))) {
            return new ApiResponse(false, "Chỉ được tạo hồ sơ cho bệnh nhân đang khám hoặc đã có kết quả xét nghiệm");
        }

        MedicalRecordEntity e = new MedicalRecordEntity();
        e.setId(UUID.randomUUID().toString());
        e.setPatientId(patient.getId());
        e.setDoctorId(request.getDoctorId());
        e.setDiagnosis(request.getDiagnosis());
        e.setNotes(request.getNotes());
        e.setCreatedAt(LocalDateTime.now());
        medicalRecordRepository.save(e);

        if (request.isRequiresTest()) {
            patient.setStatus(PatientStatus.WAITING_FOR_TEST);
            patientRepository.save(patient);
            publishRealtimeEvent(RealtimeEventType.MEDICAL_RECORD_CHANGED, e.getId(), "Hồ sơ bệnh án mới đã được tạo");
            publishRealtimeEvent(RealtimeEventType.PATIENT_CHANGED, patient.getId(), "Bệnh nhân đã chuyển sang chờ xét nghiệm");
            return new ApiResponse(true, "Đã tạo hồ sơ bệnh án và chuyển bệnh nhân sang chờ xét nghiệm");
        }

        patient.setStatus(PatientStatus.EXAMINED);
        patient.setLastVisitAt(LocalDateTime.now());
        patientRepository.save(patient);
        publishRealtimeEvent(RealtimeEventType.MEDICAL_RECORD_CHANGED, e.getId(), "Hồ sơ bệnh án mới đã được tạo");
        publishRealtimeEvent(RealtimeEventType.PATIENT_CHANGED, patient.getId(), "Bệnh nhân đã khám xong");

        return new ApiResponse(true, "Đã tạo hồ sơ bệnh án thành công");
    }

    @Override
    public List<MedicalRecord> getAllMedicalRecords() {
        return medicalRecordRepository.findAll().stream()
                .map(Mapper::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalRecord> getMedicalRecordsByPatient(String patientId) {
        PatientEntity patient = patientRepository.findByCode(patientId).orElseGet(() -> patientRepository.findById(patientId).orElse(null));
        if (patient == null) {
            return List.of();
        }
        return medicalRecordRepository.findByPatientId(patient.getId()).stream()
                .map(Mapper::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalRecord> getMedicalRecordsByDoctor(String doctorId) {
        return medicalRecordRepository.findByDoctorId(doctorId).stream()
                .map(Mapper::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public ApiResponse createPrescription(CreatePrescriptionRequest request) {
        PatientEntity patient = patientRepository.findByCode(request.getPatientId()).orElseGet(() -> patientRepository.findById(request.getPatientId()).orElse(null));
        if (patient == null) {
            return new ApiResponse(false, "Không tìm thấy bệnh nhân");
        }
        ApiResponse doctorCheck = validateActiveUserRole(request.getDoctorId(), Role.DOCTOR, "bác sĩ");
        if (!doctorCheck.isSuccess()) {
            return doctorCheck;
        }
        if (!request.getDoctorId().equals(patient.getAssignedDoctorId())) {
            return new ApiResponse(false, "Bệnh nhân không thuộc bác sĩ này");
        }
        if (patient.getStatus() == null || !(patient.getStatus().equalsIgnoreCase(PatientStatus.EXAMINED)
                || patient.getStatus().equalsIgnoreCase(PatientStatus.HAS_TEST_RESULT)
                || patient.getStatus().equalsIgnoreCase(PatientStatus.WAITING_FOR_PRESCRIPTION))) {
            return new ApiResponse(false, "Chỉ có thể kê đơn cho bệnh nhân đã khám hoặc đã có kết quả xét nghiệm");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            return new ApiResponse(false, "Đơn thuốc phải có ít nhất một thuốc");
        }

        PrescriptionEntity p = new PrescriptionEntity();
        p.setId(UUID.randomUUID().toString());
        p.setCode(generateNextPrescriptionCode());
        p.setPatientId(patient.getId());
        p.setDoctorId(request.getDoctorId());
        p.setItems(request.getItems());
        p.setStatus(PrescriptionStatus.NEW);
        p.setCreatedAt(LocalDateTime.now());
        prescriptionRepository.save(p);

        patient.setStatus(PatientStatus.PRESCRIBED);
        patient.setLastVisitAt(LocalDateTime.now());
        patientRepository.save(patient);
        publishRealtimeEvent(RealtimeEventType.PRESCRIPTION_CHANGED, p.getId(), "Toa thuốc mới đã được tạo");
        publishRealtimeEvent(RealtimeEventType.PATIENT_CHANGED, patient.getId(), "Bệnh nhân đã được kê đơn");
        return new ApiResponse(true, "Đã tạo đơn thuốc thành công và cập nhật trạng thái bệnh nhân");
    }

    @Override
    public List<Prescription> getPrescriptions() {
        return prescriptionRepository.findAll().stream()
                .map(Mapper::toModel)
                .peek(this::enrichPrescription)
                .collect(Collectors.toList());
    }

    @Override
    public List<Patient> getExaminedPatientsWithPrescriptions() {
        var patientIds = prescriptionRepository.findAll().stream()
                .map(PrescriptionEntity::getPatientId)
                .collect(Collectors.toSet());
        return patientRepository.findAll().stream()
                .filter(p -> patientIds.contains(p.getId()))
                .filter(p -> p.getStatus() != null && (
                        p.getStatus().equalsIgnoreCase(PatientStatus.EXAMINED)
                                || p.getStatus().equalsIgnoreCase(PatientStatus.PRESCRIBED)
                                || p.getStatus().equalsIgnoreCase(PatientStatus.COMPLETED)
                ))
                .map(Mapper::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Prescription> getPrescriptionsByDoctor(String doctorId) {
        return prescriptionRepository.findAll().stream()
                .map(Mapper::toModel)
                .peek(this::enrichPrescription)
                .filter(p -> doctorId.equals(p.getDoctorId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Prescription> getPrescriptionsByStatus(PrescriptionSearchRequest request) {
        String patientCode = request.getPatientCode() == null ? "" : request.getPatientCode().trim().toLowerCase();
        String doctorKeyword = request.getDoctorKeyword() == null ? "" : request.getDoctorKeyword().trim().toLowerCase();
        String status = request.getStatus() == null ? "" : request.getStatus().trim().toLowerCase();

        return prescriptionRepository.findAll().stream()
                .filter(p -> {
                    PatientEntity patient = patientRepository.findById(p.getPatientId()).orElse(null);
                    UserEntity doctor = userRepository.findById(p.getDoctorId()).orElse(null);

                    boolean matchesPatient = patientCode.isBlank() || (patient != null && patient.getCode() != null && patient.getCode().toLowerCase().contains(patientCode));
                    boolean matchesDoctor = doctorKeyword.isBlank() || (doctor != null && (
                            (doctor.getUsername() != null && doctor.getUsername().toLowerCase().contains(doctorKeyword))
                                    || (doctor.getFullName() != null && doctor.getFullName().toLowerCase().contains(doctorKeyword))
                    ));
                    boolean matchesStatus = status.isBlank() || (p.getStatus() != null && p.getStatus().toLowerCase().contains(status));

                    return matchesPatient && matchesDoctor && matchesStatus;
                })
                .map(Mapper::toModel)
                .peek(this::enrichPrescription)
                .collect(Collectors.toList());
    }

    @Override
    public ApiResponse updatePrescription(PrescriptionUpdateRequest request) {
        return prescriptionRepository.findById(request.getPrescriptionId()).map(p -> {
            if (request.getItems() == null || request.getItems().isEmpty()) {
                return new ApiResponse(false, "Toa thuốc phải có ít nhất một thuốc");
            }
            p.setItems(request.getItems());
            p.setStatus(request.getStatus());
            prescriptionRepository.save(p);
            publishRealtimeEvent(RealtimeEventType.PRESCRIPTION_CHANGED, p.getId(), "Toa thuốc đã được cập nhật");
            return new ApiResponse(true, "Đã cập nhật toa thuốc thành công");
        }).orElse(new ApiResponse(false, "Không tìm thấy toa thuốc"));
    }

    @Override
    public ApiResponse deletePrescription(String prescriptionId) {
        if (!prescriptionRepository.existsById(prescriptionId)) {
            return new ApiResponse(false, "Không tìm thấy toa thuốc");
        }
        prescriptionRepository.deleteById(prescriptionId);
        publishRealtimeEvent(RealtimeEventType.PRESCRIPTION_CHANGED, prescriptionId, "Toa thuốc đã được xóa");
        return new ApiResponse(true, "Đã xóa toa thuốc thành công");
    }

    @Override
    public ApiResponse dispensePrescription(DispensePrescriptionRequest request) {
        ApiResponse pharmacistCheck = validateActiveUserRole(request.getPharmacistId(), Role.PHARMACIST, "dược sĩ");
        if (!pharmacistCheck.isSuccess()) {
            return pharmacistCheck;
        }
        return prescriptionRepository.findById(request.getPrescriptionId()).map(p -> {
            if (PrescriptionStatus.DISPENSED.equalsIgnoreCase(p.getStatus())) {
                return new ApiResponse(false, "Toa thuốc đã được cấp phát trước đó");
            }
            p.setStatus(PrescriptionStatus.DISPENSED);
            prescriptionRepository.save(p);

            PatientEntity patient = patientRepository.findById(p.getPatientId()).orElse(null);
            if (patient != null) {
                patient.setStatus(PatientStatus.COMPLETED);
                patient.setLastVisitAt(LocalDateTime.now());
                patientRepository.save(patient);
                publishRealtimeEvent(RealtimeEventType.PATIENT_CHANGED, patient.getId(), "Bệnh nhân đã hoàn tất cấp phát thuốc");
            }
            publishRealtimeEvent(RealtimeEventType.PRESCRIPTION_CHANGED, p.getId(), "Toa thuốc đã được cấp phát");

            return new ApiResponse(true, "Đã cấp phát thuốc và cập nhật trạng thái bệnh nhân");
        }).orElse(new ApiResponse(false, "Không tìm thấy toa thuốc"));
    }

    @Override
    public ApiResponse createTestResult(TestResultRequest request) {
        PatientEntity patient = patientRepository.findByCode(request.getPatientId()).orElseGet(() -> patientRepository.findById(request.getPatientId()).orElse(null));
        if (patient == null) {
            return new ApiResponse(false, "Không tìm thấy bệnh nhân");
        }

        if (patient.getStatus() == null || !patient.getStatus().equalsIgnoreCase(PatientStatus.WAITING_FOR_TEST)) {
            return new ApiResponse(false, "Chỉ được nhập kết quả cho bệnh nhân đang ở trạng thái Chờ xét nghiệm");
        }

        String assignedDoctorId = patient.getAssignedDoctorId();
        if (assignedDoctorId == null || assignedDoctorId.isBlank()) {
            return new ApiResponse(false, "Bệnh nhân chưa được phân công bác sĩ");
        }

        ApiResponse doctorCheck = validateActiveUserRole(assignedDoctorId, Role.DOCTOR, "bác sĩ phụ trách bệnh nhân");
        if (!doctorCheck.isSuccess()) {
            return doctorCheck;
        }

        ApiResponse testingStaffCheck = validateActiveUserRole(request.getTestingStaffId(), Role.TESTING_STAFF, "nhân viên xét nghiệm");
        if (!testingStaffCheck.isSuccess()) {
            return testingStaffCheck;
        }

        TestResultEntity e = TestResultMapper.toEntity(request);
        e.setId(UUID.randomUUID().toString());
        e.setPatientId(patient.getId());
        e.setDoctorId(assignedDoctorId);
        e.setCreatedAt(LocalDateTime.now());
        testResultRepository.save(e);

        patient.setStatus(PatientStatus.HAS_TEST_RESULT);
        patient.setLastVisitAt(LocalDateTime.now());
        patientRepository.save(patient);
        publishRealtimeEvent(RealtimeEventType.TEST_RESULT_CHANGED, e.getId(), "Kết quả xét nghiệm mới đã được lưu");
        publishRealtimeEvent(RealtimeEventType.PATIENT_CHANGED, patient.getId(), "Bệnh nhân đã có kết quả xét nghiệm");
        return new ApiResponse(true, "Đã lưu kết quả xét nghiệm và chuyển bệnh nhân sang trạng thái đã có kết quả xét nghiệm");
    }

    @Override
    public List<TestResult> getTestResultsByDoctor(String doctorId) {
        return testResultRepository.findAll().stream()
                .map(TestResultMapper::toModel)
                .peek(this::enrichTestResult)
                .filter(t -> doctorId.equals(t.getDoctorId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<TestResult> getTestResultsByPatient(String patientId) {
        PatientEntity patient = patientRepository.findByCode(patientId).orElseGet(() -> patientRepository.findById(patientId).orElse(null));
        if (patient == null) {
            return List.of();
        }
        return testResultRepository.findAll().stream()
                .map(TestResultMapper::toModel)
                .peek(this::enrichTestResult)
                .filter(t -> patient.getId().equals(t.getPatientId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll().stream().map(UserMapper::toModel).collect(Collectors.toList());
    }

    @Override
    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role).stream().map(UserMapper::toModel).collect(Collectors.toList());
    }

    @Override
    public List<User> searchUsers(UserSearchRequest request) {
        String keyword = request.getKeyword() == null ? "" : request.getKeyword().toLowerCase();
        return userRepository.findAll().stream()
                .map(UserMapper::toModel)
                .filter(u -> u.getUsername().toLowerCase().contains(keyword)
                        || u.getFullName().toLowerCase().contains(keyword)
                        || u.getRole().name().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
    }

    @Override
    public ApiResponse createUser(UserRequest request) {
        if (request == null || isBlank(request.getUsername()) || isBlank(request.getPassword()) || request.getRole() == null) {
            return new ApiResponse(false, "Tên đăng nhập, mật khẩu và vai trò không được để trống");
        }
        if (userRepository.existsByUsername(trim(request.getUsername()))) {
            return new ApiResponse(false, "Tên đăng nhập đã tồn tại");
        }
        UserEntity e = UserMapper.toEntity(request);
        if (e.getId() == null || e.getId().isBlank()) {
            e.setId(UUID.randomUUID().toString());
        }
        e.setUsername(trim(request.getUsername()));
        e.setFullName(trim(request.getFullName()));
        userRepository.save(e);
        publishRealtimeEvent(RealtimeEventType.USER_CHANGED, e.getId(), "Người dùng mới đã được tạo");
        return new ApiResponse(true, "Đã tạo người dùng thành công");
    }

    @Override
    public ApiResponse updateUser(UserRequest request) {
        if (request == null || isBlank(request.getId()) || isBlank(request.getUsername()) || request.getRole() == null) {
            return new ApiResponse(false, "Thiếu thông tin người dùng cần cập nhật");
        }
        return userRepository.findById(request.getId()).map(e -> {
            UserEntity duplicated = userRepository.findByUsername(trim(request.getUsername())).orElse(null);
            if (duplicated != null && !duplicated.getId().equals(request.getId())) {
                return new ApiResponse(false, "Tên đăng nhập đã tồn tại");
            }
            e.setUsername(trim(request.getUsername()));
            if (!isBlank(request.getPassword())) {
                e.setPassword(request.getPassword());
            }
            e.setRole(request.getRole());
            e.setFullName(trim(request.getFullName()));
            e.setActive(request.isActive());
            userRepository.save(e);
            publishRealtimeEvent(RealtimeEventType.USER_CHANGED, e.getId(), "Người dùng đã được cập nhật");
            return new ApiResponse(true, "Đã cập nhật người dùng thành công");
        }).orElse(new ApiResponse(false, "Không tìm thấy người dùng"));
    }

    @Override
    public ApiResponse deleteUser(String userId) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return new ApiResponse(false, "Không tìm thấy người dùng");
        }

        boolean assignedToPatients = patientRepository.findAll().stream()
                .anyMatch(p -> userId.equals(p.getAssignedDoctorId()));
        if (assignedToPatients) {
            return new ApiResponse(false, "Không thể xóa người dùng vì đang được gán cho bệnh nhân");
        }

        boolean hasMedicalRecords = medicalRecordRepository.findAll().stream()
                .anyMatch(r -> userId.equals(r.getDoctorId()));
        if (hasMedicalRecords) {
            return new ApiResponse(false, "Không thể xóa người dùng vì còn hồ sơ bệnh án liên quan");
        }

        boolean hasPrescriptions = prescriptionRepository.findAll().stream()
                .anyMatch(p -> userId.equals(p.getDoctorId()));
        if (hasPrescriptions) {
            return new ApiResponse(false, "Không thể xóa người dùng vì còn toa thuốc liên quan");
        }

        boolean hasTestResults = testResultRepository.findAll().stream()
                .anyMatch(t -> userId.equals(t.getDoctorId()) || userId.equals(t.getTestingStaffId()));
        if (hasTestResults) {
            return new ApiResponse(false, "Không thể xóa người dùng vì còn kết quả xét nghiệm liên quan");
        }

        userRepository.deleteById(userId);
        publishRealtimeEvent(RealtimeEventType.USER_CHANGED, userId, "Người dùng đã được xóa");
        return new ApiResponse(true, "Đã xóa người dùng thành công");
    }

    private void publishRealtimeEvent(String type, String entityId, String message) {
        realtimeEventPublisher.publish(type, entityId, message);
    }

    private ApiResponse validateActiveUserRole(String userId, Role expectedRole, String label) {
        if (isBlank(userId)) {
            return new ApiResponse(false, "Thiếu thông tin " + label);
        }
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return new ApiResponse(false, "Không tìm thấy " + label);
        }
        if (!user.isActive()) {
            return new ApiResponse(false, label + " đang bị khóa");
        }
        if (user.getRole() != expectedRole) {
            return new ApiResponse(false, "Người dùng được chọn không phải là " + label);
        }
        return new ApiResponse(true, "OK");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void enrichPrescription(Prescription prescription) {
        if (prescription == null) {
            return;
        }
        PatientEntity patient = patientRepository.findById(prescription.getPatientId()).orElse(null);
        if (patient != null) {
            prescription.setPatientCode(patient.getCode());
        }
        UserEntity doctor = userRepository.findById(prescription.getDoctorId()).orElse(null);
        if (doctor != null) {
            prescription.setDoctorCode(doctor.getUsername());
        }
    }

    private void enrichTestResult(TestResult testResult) {
        if (testResult == null) {
            return;
        }
        PatientEntity patient = patientRepository.findById(testResult.getPatientId()).orElse(null);
        if (patient != null) {
            testResult.setPatientCode(patient.getCode());
        }
        UserEntity doctor = userRepository.findById(testResult.getDoctorId()).orElse(null);
        if (doctor != null) {
            testResult.setDoctorCode(doctor.getUsername());
        }
    }

    private String generateNextPrescriptionCode() {
        int max = prescriptionRepository.findAll().stream()
                .map(PrescriptionEntity::getCode)
                .filter(code -> code != null && code.matches("DT\\d+"))
                .map(code -> Integer.parseInt(code.substring(2)))
                .max(Integer::compareTo)
                .orElse(0);
        return String.format("DT%03d", max + 1);
    }
}
