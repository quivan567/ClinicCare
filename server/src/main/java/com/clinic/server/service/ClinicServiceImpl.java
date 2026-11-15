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
import com.clinic.shared.dto.IntakeParseResult;
import com.clinic.shared.dto.LoginRequest;
import com.clinic.shared.dto.LoginResponse;
import com.clinic.shared.dto.MedicalRecordRequest;
import com.clinic.shared.dto.MedicalRecordSuggestion;
import com.clinic.shared.dto.PatientSummaryResponse;
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
import com.clinic.shared.model.PrescriptionItem;
import com.clinic.shared.model.PrescriptionStatus;
import com.clinic.shared.model.Role;
import com.clinic.shared.model.TestResult;
import com.clinic.shared.model.User;
import com.clinic.shared.rmi.ClinicService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClinicServiceImpl extends UnicastRemoteObject implements ClinicService {
    private static final DateTimeFormatter HISTORY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final TestResultRepository testResultRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final AiAssistantService aiAssistantService;

    @Autowired
    public ClinicServiceImpl(UserRepository userRepository, PatientRepository patientRepository,
                             MedicalRecordRepository medicalRecordRepository, PrescriptionRepository prescriptionRepository,
                             TestResultRepository testResultRepository,
                             RealtimeEventPublisher realtimeEventPublisher,
                             PlatformTransactionManager transactionManager,
                             JdbcTemplate jdbcTemplate,
                             AiAssistantService aiAssistantService) throws RemoteException {
        super();
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.testResultRepository = testResultRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.jdbcTemplate = jdbcTemplate;
        this.aiAssistantService = aiAssistantService;
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
        if (medicalRecordRepository.existsByPatientId(patient.getId())) {
            return new ApiResponse(false, "Không thể xóa bệnh nhân vì còn hồ sơ bệnh án liên quan");
        }
        if (prescriptionRepository.existsByPatientId(patient.getId())) {
            return new ApiResponse(false, "Không thể xóa bệnh nhân vì còn toa thuốc liên quan");
        }
        if (testResultRepository.existsByPatientId(patient.getId())) {
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
        return toPatients(patientRepository.findAll());
    }

    @Override
    public List<Patient> getPatientsByDoctor(String doctorId) {
        return toPatients(patientRepository.findByAssignedDoctorId(doctorId));
    }

    @Override
    public List<Patient> getWaitingPatientsByDoctor(String doctorId) {
        return toPatients(patientRepository.findByAssignedDoctorIdAndStatusIn(
                doctorId, List.of(PatientStatus.WAITING, PatientStatus.ASSIGNED)));
    }

    @Override
    public List<Patient> getPatientsByStatus(String status) {
        if (isBlank(status)) {
            return List.of();
        }
        return toPatients(patientRepository.findByStatus(status.trim()));
    }

    @Override
    public ApiResponse acceptPatient(String patientCode, String doctorId) {
        PatientEntity patient = findPatient(patientCode);
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
        String keyword = request.getKeyword() == null ? "" : request.getKeyword().trim();
        if (keyword.isEmpty()) {
            return getAllPatients();
        }
        return toPatients(patientRepository.searchByKeyword(likeContains(keyword)));
    }

    @Override
    public ApiResponse assignDoctor(AssignDoctorRequest request) {
        PatientEntity target = findPatient(request.getPatientId());
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
        PatientEntity patient = findPatient(request.getPatientId());
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
        return toMedicalRecords(medicalRecordRepository.findAll());
    }

    @Override
    public List<MedicalRecord> getMedicalRecordsByPatient(String patientId) {
        PatientEntity patient = findPatient(patientId);
        if (patient == null) {
            return List.of();
        }
        return toMedicalRecords(medicalRecordRepository.findByPatientId(patient.getId()));
    }

    @Override
    public List<MedicalRecord> getMedicalRecordsByDoctor(String doctorId) {
        return toMedicalRecords(medicalRecordRepository.findByDoctorId(doctorId));
    }

    @Override
    public PatientSummaryResponse summarizePatient(String patientId, String doctorId) {
        if (!aiAssistantService.isConfigured()) {
            return PatientSummaryResponse.fail(aiAssistantService.disabledMessage());
        }
        ApiResponse doctorCheck = validateActiveUserRole(doctorId, Role.DOCTOR, "bác sĩ");
        if (!doctorCheck.isSuccess()) {
            return PatientSummaryResponse.fail(doctorCheck.getMessage());
        }
        PatientEntity patient = findPatient(patientId);
        if (patient == null) {
            return PatientSummaryResponse.fail("Không tìm thấy bệnh nhân");
        }
        if (!doctorId.equals(patient.getAssignedDoctorId())) {
            return PatientSummaryResponse.fail("Bệnh nhân không thuộc bác sĩ này");
        }
        try {
            String history = buildPatientHistory(patient);
            JsonNode node = aiAssistantService.completeJson(
                    "Bạn là trợ lý phòng khám. Chỉ tóm tắt dữ liệu được cung cấp, không bịa bệnh, thuốc hoặc kết quả. "
                            + "Trả về đúng một JSON {\"summary\":\"một đoạn văn tiếng Việt\"}. Không dùng markdown.",
                    history);
            String summary = jsonText(node, "summary");
            if (isBlank(summary)) {
                return PatientSummaryResponse.fail("Trợ lý AI không tạo được tóm tắt");
            }
            return PatientSummaryResponse.ok(summary);
        } catch (AiCallException ex) {
            return PatientSummaryResponse.fail(ex.getMessage());
        }
    }

    @Override
    public MedicalRecordSuggestion suggestMedicalRecord(String doctorId, String notes) {
        if (!aiAssistantService.isConfigured()) {
            return MedicalRecordSuggestion.fail(aiAssistantService.disabledMessage());
        }
        ApiResponse doctorCheck = validateActiveUserRole(doctorId, Role.DOCTOR, "bác sĩ");
        if (!doctorCheck.isSuccess()) {
            return MedicalRecordSuggestion.fail(doctorCheck.getMessage());
        }
        if (isBlank(notes)) {
            return MedicalRecordSuggestion.fail("Ghi chú không được để trống");
        }
        try {
            JsonNode node = aiAssistantService.completeJson(
                    "Bạn là trợ lý phòng khám. Đọc ghi chú khám và gợi ý chẩn đoán. "
                            + "Không kê thuốc. Không khẳng định chắc chắn khi ghi chú chưa đủ. "
                            + "Trả về đúng một JSON {\"diagnosis\":\"\",\"notes\":\"\",\"requiresTest\":false}. "
                            + "notes là ghi chú đã viết lại cho rõ, bằng tiếng Việt. Không dùng markdown.",
                    "Ghi chú khám:\n" + clip(notes, 2000));
            String diagnosis = jsonText(node, "diagnosis");
            if (isBlank(diagnosis)) {
                return MedicalRecordSuggestion.fail("Trợ lý AI không tạo được gợi ý chẩn đoán");
            }
            return MedicalRecordSuggestion.ok(diagnosis, jsonText(node, "notes"), node.path("requiresTest").asBoolean(false));
        } catch (AiCallException ex) {
            return MedicalRecordSuggestion.fail(ex.getMessage());
        }
    }

    @Override
    public IntakeParseResult parseIntakeText(String receptionistId, String text) {
        if (!aiAssistantService.isConfigured()) {
            return IntakeParseResult.fail(aiAssistantService.disabledMessage());
        }
        ApiResponse receptionistCheck = validateActiveUserRole(receptionistId, Role.RECEPTIONIST, "lễ tân");
        if (!receptionistCheck.isSuccess()) {
            return IntakeParseResult.fail(receptionistCheck.getMessage());
        }
        if (isBlank(text)) {
            return IntakeParseResult.fail("Mô tả không được để trống");
        }
        try {
            JsonNode node = aiAssistantService.completeJson(
                    "Bạn tách thông tin tiếp nhận bệnh nhân từ một câu tiếng Việt. "
                            + "Không bịa thông tin không có trong câu. Không tạo mã bệnh nhân hay bác sĩ. "
                            + "Trả về đúng một JSON {\"fullName\":\"\",\"age\":0,\"gender\":\"\",\"phone\":\"\",\"address\":\"\"}. "
                            + "gender chỉ được là Nam, Nữ hoặc Khác. age bằng 0 nếu không có tuổi. Không dùng markdown.",
                    "Mô tả:\n" + clip(text, 1000));
            String fullName = jsonText(node, "fullName");
            Integer age = jsonAge(node);
            String gender = normalizeGender(jsonText(node, "gender"));
            String phone = jsonText(node, "phone");
            String address = jsonText(node, "address");
            if (isBlank(fullName) && age == null && isBlank(gender) && isBlank(phone) && isBlank(address)) {
                return IntakeParseResult.fail("Không tách được thông tin từ mô tả");
            }
            return IntakeParseResult.ok(fullName, age, gender, phone, address);
        } catch (AiCallException ex) {
            return IntakeParseResult.fail(ex.getMessage());
        }
    }

    @Override
    public ApiResponse createPrescription(CreatePrescriptionRequest request) {
        PatientEntity patient = findPatient(request.getPatientId());
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
        p.replaceItems(request.getItems());
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
        return toPrescriptions(prescriptionRepository.findAll());
    }

    @Override
    public List<Patient> getExaminedPatientsWithPrescriptions() {
        List<PatientEntity> candidates = patientRepository.findByStatusIn(List.of(
                PatientStatus.EXAMINED, PatientStatus.PRESCRIBED, PatientStatus.COMPLETED));
        if (candidates.isEmpty()) {
            return List.of();
        }
        Set<String> patientIds = candidates.stream().map(PatientEntity::getId).collect(Collectors.toSet());
        Set<String> withPrescription = prescriptionRepository.findByPatientIdIn(patientIds).stream()
                .map(PrescriptionEntity::getPatientId)
                .collect(Collectors.toSet());
        List<PatientEntity> matched = candidates.stream()
                .filter(patient -> withPrescription.contains(patient.getId()))
                .toList();
        return toPatients(matched);
    }

    @Override
    public List<Prescription> getPrescriptionsByDoctor(String doctorId) {
        return toPrescriptions(prescriptionRepository.findByDoctorId(doctorId));
    }

    @Override
    public List<Prescription> getPrescriptionsByStatus(PrescriptionSearchRequest request) {
        String patientCode = request.getPatientCode() == null ? "" : request.getPatientCode().trim();
        String doctorKeyword = request.getDoctorKeyword() == null ? "" : request.getDoctorKeyword().trim();
        String status = request.getStatus() == null ? "" : request.getStatus().trim();

        List<String> patientIds = null;
        if (!patientCode.isEmpty()) {
            patientIds = patientRepository.findByCodePattern(likeContains(patientCode)).stream()
                    .map(PatientEntity::getId)
                    .toList();
            if (patientIds.isEmpty()) {
                return List.of();
            }
        }

        List<String> doctorIds = null;
        if (!doctorKeyword.isEmpty()) {
            doctorIds = userRepository.findByUsernameOrFullName(likeContains(doctorKeyword)).stream()
                    .map(UserEntity::getId)
                    .toList();
            if (doctorIds.isEmpty()) {
                return List.of();
            }
        }

        List<PrescriptionEntity> prescriptions;
        if (patientIds != null && doctorIds != null) {
            prescriptions = prescriptionRepository.findByPatientIdInAndDoctorIdIn(patientIds, doctorIds);
        } else if (patientIds != null) {
            prescriptions = prescriptionRepository.findByPatientIdIn(patientIds);
        } else if (doctorIds != null) {
            prescriptions = prescriptionRepository.findByDoctorIdIn(doctorIds);
        } else if (!status.isEmpty()) {
            prescriptions = prescriptionRepository.findByStatusPattern(likeContains(status));
        } else {
            prescriptions = prescriptionRepository.findAll();
        }

        if (!status.isEmpty() && (patientIds != null || doctorIds != null)) {
            String statusKeyword = status.toLowerCase();
            prescriptions = prescriptions.stream()
                    .filter(prescription -> prescription.getStatus() != null
                            && prescription.getStatus().toLowerCase().contains(statusKeyword))
                    .toList();
        }
        return toPrescriptions(prescriptions);
    }

    @Override
    public ApiResponse updatePrescription(PrescriptionUpdateRequest request) {
        ApiResponse response = transactionTemplate.execute(status -> prescriptionRepository.findById(request.getPrescriptionId()).map(prescription -> {
            if (request.getItems() == null || request.getItems().isEmpty()) {
                return new ApiResponse(false, "Toa thuốc phải có ít nhất một thuốc");
            }
            prescription.replaceItems(request.getItems());
            prescription.setStatus(request.getStatus());
            prescriptionRepository.save(prescription);
            return new ApiResponse(true, "Đã cập nhật toa thuốc thành công");
        }).orElse(new ApiResponse(false, "Không tìm thấy toa thuốc")));
        if (response != null && response.isSuccess()) {
            publishRealtimeEvent(RealtimeEventType.PRESCRIPTION_CHANGED, request.getPrescriptionId(), "Toa thuốc đã được cập nhật");
        }
        return response == null ? new ApiResponse(false, "Không thể cập nhật toa thuốc") : response;
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
        PatientEntity patient = findPatient(request.getPatientId());
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
        return toTestResults(testResultRepository.findByDoctorId(doctorId));
    }

    @Override
    public List<TestResult> getTestResultsByPatient(String patientId) {
        PatientEntity patient = findPatient(patientId);
        if (patient == null) {
            return List.of();
        }
        return toTestResults(testResultRepository.findByPatientId(patient.getId()));
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
        String keyword = request.getKeyword() == null ? "" : request.getKeyword().trim();
        if (keyword.isEmpty()) {
            return getAllUsers();
        }
        return userRepository.searchByKeyword(likeContains(keyword)).stream()
                .map(UserMapper::toModel)
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

        boolean assignedToPatients = patientRepository.existsByAssignedDoctorId(userId);
        if (assignedToPatients) {
            return new ApiResponse(false, "Không thể xóa người dùng vì đang được gán cho bệnh nhân");
        }

        boolean hasMedicalRecords = medicalRecordRepository.existsByDoctorId(userId);
        if (hasMedicalRecords) {
            return new ApiResponse(false, "Không thể xóa người dùng vì còn hồ sơ bệnh án liên quan");
        }

        boolean hasPrescriptions = prescriptionRepository.existsByDoctorId(userId);
        if (hasPrescriptions) {
            return new ApiResponse(false, "Không thể xóa người dùng vì còn toa thuốc liên quan");
        }

        boolean hasTestResults = testResultRepository.existsByDoctorId(userId)
                || testResultRepository.existsByTestingStaffId(userId);
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

    private String buildPatientHistory(PatientEntity patient) {
        String patientKey = patient.getId();
        List<MedicalRecord> records = latest(
                toMedicalRecords(medicalRecordRepository.findByPatientId(patientKey)),
                MedicalRecord::getCreatedAt, 8);
        List<TestResult> tests = latest(
                toTestResults(testResultRepository.findByPatientId(patientKey)),
                TestResult::getCreatedAt, 8);
        List<Prescription> prescriptions = latest(
                toPrescriptions(prescriptionRepository.findByPatientId(patientKey)),
                Prescription::getCreatedAt, 8);

        StringBuilder history = new StringBuilder();
        history.append("Bệnh nhân: ").append(clip(patient.getFullName(), 120))
                .append(", tuổi ").append(patient.getAge())
                .append(", giới tính ").append(clip(patient.getGender(), 20))
                .append(".\nHồ sơ cũ:\n");
        if (records.isEmpty()) {
            history.append("- Chưa có hồ sơ.\n");
        }
        for (MedicalRecord record : records) {
            history.append("- ").append(formatTime(record.getCreatedAt()))
                    .append(" | Chẩn đoán: ").append(clip(record.getDiagnosis(), 300))
                    .append(" | Ghi chú: ").append(clip(record.getNotes(), 300))
                    .append('\n');
        }
        history.append("Xét nghiệm:\n");
        if (tests.isEmpty()) {
            history.append("- Chưa có kết quả.\n");
        }
        for (TestResult test : tests) {
            history.append("- ").append(formatTime(test.getCreatedAt()))
                    .append(" | ").append(clip(test.getTestName(), 120))
                    .append(": ").append(clip(test.getResult(), 300))
                    .append('\n');
        }
        history.append("Toa thuốc:\n");
        if (prescriptions.isEmpty()) {
            history.append("- Chưa có toa.\n");
        }
        for (Prescription prescription : prescriptions) {
            history.append("- ").append(formatTime(prescription.getCreatedAt())).append(" | ");
            List<PrescriptionItem> items = prescription.getItems();
            if (items == null || items.isEmpty()) {
                history.append("Không có thuốc");
            } else {
                for (int i = 0; i < items.size(); i++) {
                    PrescriptionItem item = items.get(i);
                    if (i > 0) {
                        history.append("; ");
                    }
                    history.append(clip(item.getMedicineName(), 80))
                            .append(' ').append(clip(item.getDosage(), 80));
                }
            }
            history.append('\n');
        }
        return history.toString();
    }

    private <T> List<T> latest(List<T> items, Function<T, LocalDateTime> time, int limit) {
        return items.stream()
                .sorted(Comparator.comparing(time, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(limit)
                .toList();
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "" : value.format(HISTORY_TIME);
    }

    private String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().replace('\n', ' ').replace('\r', ' ');
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max) + "...";
    }

    private String jsonText(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return "";
        }
        return node.get(field).asText("").trim();
    }

    private Integer jsonAge(JsonNode node) {
        JsonNode ageNode = node == null ? null : node.get("age");
        if (ageNode == null || ageNode.isNull()) {
            return null;
        }
        int age = ageNode.isNumber() ? ageNode.asInt() : parseAge(ageNode.asText(""));
        return age > 0 && age < 150 ? age : null;
    }

    private int parseAge(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ex) {
            return 0;
        }
    }

    private String normalizeGender(String value) {
        if (isBlank(value)) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("nam") || normalized.equals("male")) {
            return "Nam";
        }
        if (normalized.equals("nữ") || normalized.equals("nu") || normalized.equals("female")) {
            return "Nữ";
        }
        if (normalized.equals("khác") || normalized.equals("khac") || normalized.equals("other")) {
            return "Khác";
        }
        return "";
    }

    private PatientEntity findPatient(String value) {
        if (isBlank(value)) {
            return null;
        }
        String key = value.trim();
        return patientRepository.findByCode(key).or(() -> patientRepository.findById(key)).orElse(null);
    }

    private List<Patient> toPatients(List<PatientEntity> entities) {
        List<Patient> patients = entities.stream().map(Mapper::toModel).toList();
        Map<String, UserEntity> doctors = loadUsers(patients.stream()
                .map(Patient::getAssignedDoctorId)
                .filter(id -> !isBlank(id))
                .collect(Collectors.toSet()));
        for (Patient patient : patients) {
            UserEntity doctor = doctors.get(patient.getAssignedDoctorId());
            if (doctor != null) {
                patient.setAssignedDoctorName(doctor.getFullName());
            }
        }
        return patients;
    }

    private List<Prescription> toPrescriptions(List<PrescriptionEntity> entities) {
        Map<String, PatientEntity> patients = loadPatients(entities.stream().map(PrescriptionEntity::getPatientId).collect(Collectors.toSet()));
        Map<String, UserEntity> doctors = loadUsers(entities.stream().map(PrescriptionEntity::getDoctorId).collect(Collectors.toSet()));
        return entities.stream().map(entity -> {
            Prescription prescription = Mapper.toModel(entity);
            PatientEntity patient = patients.get(entity.getPatientId());
            if (patient != null) {
                prescription.setPatientCode(patient.getCode());
                prescription.setPatientName(patient.getFullName());
            }
            UserEntity doctor = doctors.get(entity.getDoctorId());
            if (doctor != null) {
                prescription.setDoctorCode(doctor.getUsername());
                prescription.setDoctorName(doctor.getFullName());
            }
            return prescription;
        }).toList();
    }

    private List<MedicalRecord> toMedicalRecords(List<MedicalRecordEntity> entities) {
        Map<String, PatientEntity> patients = loadPatients(entities.stream().map(MedicalRecordEntity::getPatientId).collect(Collectors.toSet()));
        Map<String, UserEntity> doctors = loadUsers(entities.stream().map(MedicalRecordEntity::getDoctorId).collect(Collectors.toSet()));
        return entities.stream().map(entity -> {
            MedicalRecord record = Mapper.toModel(entity);
            PatientEntity patient = patients.get(entity.getPatientId());
            if (patient != null) {
                record.setPatientCode(patient.getCode());
                record.setPatientName(patient.getFullName());
            }
            UserEntity doctor = doctors.get(entity.getDoctorId());
            if (doctor != null) {
                record.setDoctorName(doctor.getFullName());
            }
            return record;
        }).toList();
    }

    private List<TestResult> toTestResults(List<TestResultEntity> entities) {
        Map<String, PatientEntity> patients = loadPatients(entities.stream().map(TestResultEntity::getPatientId).collect(Collectors.toSet()));
        Map<String, UserEntity> doctors = loadUsers(entities.stream().map(TestResultEntity::getDoctorId).collect(Collectors.toSet()));
        return entities.stream().map(entity -> {
            TestResult testResult = TestResultMapper.toModel(entity);
            PatientEntity patient = patients.get(entity.getPatientId());
            if (patient != null) {
                testResult.setPatientCode(patient.getCode());
                testResult.setPatientName(patient.getFullName());
            }
            UserEntity doctor = doctors.get(entity.getDoctorId());
            if (doctor != null) {
                testResult.setDoctorCode(doctor.getUsername());
                testResult.setDoctorName(doctor.getFullName());
            }
            return testResult;
        }).toList();
    }

    private Map<String, PatientEntity> loadPatients(Collection<String> ids) {
        Set<String> keys = normalizeIds(ids);
        if (keys.isEmpty()) {
            return Map.of();
        }
        return patientRepository.findAllById(keys).stream()
                .collect(Collectors.toMap(PatientEntity::getId, Function.identity()));
    }

    private Map<String, UserEntity> loadUsers(Collection<String> ids) {
        Set<String> keys = normalizeIds(ids);
        if (keys.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(keys).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }

    private Set<String> normalizeIds(Collection<String> ids) {
        Set<String> keys = new HashSet<>();
        if (ids == null) {
            return keys;
        }
        for (String id : ids) {
            if (!isBlank(id)) {
                keys.add(id);
            }
        }
        return keys;
    }

    private String likeContains(String keyword) {
        String escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private String generateNextPrescriptionCode() {
        Long sequence = transactionTemplate.execute(status -> {
            jdbcTemplate.update("""
                    INSERT INTO counters (id, seq)
                    SELECT 'prescription', COALESCE(MAX(CAST(SUBSTRING(code, 3) AS UNSIGNED)), 0)
                    FROM prescriptions
                    WHERE code REGEXP '^DT[0-9]+$'
                    ON DUPLICATE KEY UPDATE id = id
                    """);
            jdbcTemplate.update("UPDATE counters SET seq = LAST_INSERT_ID(seq + 1) WHERE id = 'prescription'");
            return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        });
        long value = sequence == null ? 1 : sequence;
        return String.format("DT%03d", value);
    }
}
