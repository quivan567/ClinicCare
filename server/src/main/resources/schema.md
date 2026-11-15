# MariaDB tables

Database: `clinic_system`

## users
- id
- username (unique)
- password
- role
- full_name
- active

## patients
- id
- code (unique)
- full_name
- age
- gender
- phone
- address
- assigned_doctor_id
- status
- last_visit_at

## medical_records
- id
- patient_id
- doctor_id
- diagnosis
- notes
- created_at

## prescriptions
- id
- code (unique)
- patient_id
- doctor_id
- status
- created_at

## prescription_items
- id
- prescription_id
- line_no
- medicine_name
- dosage
- instructions

## test_results
- id
- patient_id
- doctor_id
- testing_staff_id
- test_name
- result_text
- notes
- created_at

## counters
- id
- seq
