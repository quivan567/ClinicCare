# MongoDB Collections

## users
- _id
- username
- password
- role
- fullName
- active

## patients
- _id
- code
- fullName
- age
- gender
- phone
- address
- assignedDoctorId
- status

## medical_records
- _id
- patientId
- doctorId
- diagnosis
- notes
- createdAt

## prescriptions
- _id
- patientId
- doctorId
- medicineName
- dosage
- instructions
- status
- createdAt
