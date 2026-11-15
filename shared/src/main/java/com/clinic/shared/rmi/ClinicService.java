package com.clinic.shared.rmi;

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
import com.clinic.shared.dto.ReceptionIntakeRequest;
import com.clinic.shared.dto.SearchPatientRequest;
import com.clinic.shared.dto.TestResultRequest;
import com.clinic.shared.dto.UserRequest;
import com.clinic.shared.dto.UserSearchRequest;
import com.clinic.shared.model.MedicalRecord;
import com.clinic.shared.model.Patient;
import com.clinic.shared.model.Prescription;
import com.clinic.shared.model.Role;
import com.clinic.shared.model.TestResult;
import com.clinic.shared.model.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ClinicService extends Remote {
    LoginResponse login(LoginRequest request) throws RemoteException;

    User getMyProfile(String userId) throws RemoteException;

    ApiResponse changePassword(ChangePasswordRequest request) throws RemoteException;

    ApiResponse receivePatient(ReceptionIntakeRequest request) throws RemoteException;

    ApiResponse addPatient(Patient patient) throws RemoteException;

    ApiResponse updatePatient(Patient patient) throws RemoteException;

    ApiResponse deletePatient(String patientCode) throws RemoteException;

    List<Patient> getAllPatients() throws RemoteException;

    List<Patient> getPatientsByDoctor(String doctorId) throws RemoteException;

    List<Patient> getWaitingPatientsByDoctor(String doctorId) throws RemoteException;

    List<Patient> getPatientsByStatus(String status) throws RemoteException;

    ApiResponse acceptPatient(String patientCode, String doctorId) throws RemoteException;

    List<Patient> searchPatients(SearchPatientRequest request) throws RemoteException;

    ApiResponse assignDoctor(AssignDoctorRequest request) throws RemoteException;

    ApiResponse createMedicalRecord(MedicalRecordRequest request) throws RemoteException;

    PatientSummaryResponse summarizePatient(String patientId, String doctorId) throws RemoteException;

    MedicalRecordSuggestion suggestMedicalRecord(String doctorId, String notes) throws RemoteException;

    IntakeParseResult parseIntakeText(String receptionistId, String text) throws RemoteException;

    List<MedicalRecord> getAllMedicalRecords() throws RemoteException;

    List<MedicalRecord> getMedicalRecordsByPatient(String patientId) throws RemoteException;

    List<MedicalRecord> getMedicalRecordsByDoctor(String doctorId) throws RemoteException;

    ApiResponse createPrescription(CreatePrescriptionRequest request) throws RemoteException;

    List<Prescription> getPrescriptions() throws RemoteException;

    List<Patient> getExaminedPatientsWithPrescriptions() throws RemoteException;

    List<Prescription> getPrescriptionsByDoctor(String doctorId) throws RemoteException;

    List<Prescription> getPrescriptionsByStatus(PrescriptionSearchRequest request) throws RemoteException;

    ApiResponse updatePrescription(PrescriptionUpdateRequest request) throws RemoteException;

    ApiResponse deletePrescription(String prescriptionId) throws RemoteException;

    ApiResponse dispensePrescription(DispensePrescriptionRequest request) throws RemoteException;

    ApiResponse createTestResult(TestResultRequest request) throws RemoteException;

    List<TestResult> getTestResultsByDoctor(String doctorId) throws RemoteException;

    List<TestResult> getTestResultsByPatient(String patientId) throws RemoteException;

    List<User> getAllUsers() throws RemoteException;

    List<User> getUsersByRole(Role role) throws RemoteException;

    List<User> searchUsers(UserSearchRequest request) throws RemoteException;

    ApiResponse createUser(UserRequest request) throws RemoteException;

    ApiResponse updateUser(UserRequest request) throws RemoteException;

    ApiResponse deleteUser(String userId) throws RemoteException;
}
