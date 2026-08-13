-- ==========================================
-- SEED USERS
-- ==========================================
INSERT INTO public.users (id, is_active, created_at, updated_at, role, email, password_hash) VALUES 
('11111111-1111-1111-1111-111111111111', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ADMIN', 'admin.suprem@msg.group', '$2a$10$/tnSZEkzPI56CdKX4FDLveWuD7ZpOuw.SYw./jxXyUeNk5CcSvejW'),
('22222222-2222-2222-2222-222222222222', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'MARKETING_ORGANIZER', 'maria.marketing@msg.group', '$2a$10$ZFvATBn6GboMb/keGrLQlOIvnTCVhMO7zkFqj6qCcJQjjjkrjx64u'),
('33333333-3333-3333-3333-333333333333', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'HR_USER', 'elena.hr@msg.group', '$2a$10$y5.v4g8e3BH7Ut0oLYLpae.U6JKk/Kh6skMxnW481dtWaOtHcost6'),
('44444444-4444-4444-4444-444444444444', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PARTICIPANT', 'andrei.popescu@msg.group', '$2a$10$OCfv5wlctiS0W8X9WvmWae8LlkMiwxUUxjne/8cqpMGTGpwrnDPES'),
('55555555-5555-5555-5555-555555555555', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PARTICIPANT', 'mihai.ionescu@msg.group', '$2a$10$1s9ntGiMENvWOUerAvn8EeDtVyLygCzW1eI3hpsvNEgyG9kyZthZG'),
('66666666-6666-6666-6666-666666666666', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PARTICIPANT', 'fost.angajat@msg.group', '$2a$10$4jf53daaLy75hEzc7Ah35.TpMVozt3YJWOtL3CAT85zCuBVD0Gkj2')
ON CONFLICT (id) DO UPDATE 
SET is_active = EXCLUDED.is_active,
    role = EXCLUDED.role,
    email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    updated_at = CURRENT_TIMESTAMP;

-- ==========================================
-- SEED USER DETAILS
-- ==========================================
INSERT INTO public.user_details (id, user_id, location, first_name, last_name, profile_picture) VALUES 
('3f66566c-06a1-4ca8-9e0f-4b26b24ea492', '11111111-1111-1111-1111-111111111111', 'CLUJ', 'Admin', 'Suprem', NULL),
('0ad0e1b3-f587-4fce-887f-8d889fa83eb8', '22222222-2222-2222-2222-222222222222', 'TIMISOARA', 'Maria', 'Marketing', NULL),
('7c7a56f8-afc6-4bab-9d4f-267045d20b1a', '33333333-3333-3333-3333-333333333333', 'MURES', 'Elena', 'ResurseUmane', NULL),
('3e1e39cf-762b-4c19-a184-6866484fb551', '44444444-4444-4444-4444-444444444444', 'CLUJ', 'Andrei', 'Popescu', NULL),
('f9715164-f911-4487-a55a-09fcfe7fc4b0', '55555555-5555-5555-5555-555555555555', 'MURES', 'Mihai', 'Ionescu', NULL),
('76cb668e-0876-4983-b7d3-5fcaf5d926c8', '66666666-6666-6666-6666-666666666666', 'TIMISOARA', 'Fost', 'Angajat', NULL)
ON CONFLICT (id) DO UPDATE
SET location = EXCLUDED.location,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name;

-- ==========================================
-- SEED EVENTS (Example Data)
-- ==========================================
INSERT INTO public.events (id, created_at, updated_at, created_by, name, location, type, status, event_start_date, event_end_time, registration_end_date) VALUES 
('a1111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '11111111-1111-1111-1111-111111111111', 'MSG Tech Days 2026', 'CLUJ', 'INTERNAL', 'PUBLISHED', '2026-09-15 09:00:00+02', '2026-09-16 18:00:00+02', '2026-09-01 23:59:59+02'),
('a2222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '22222222-2222-2222-2222-222222222222', 'Marketing Workshop', 'TIMISOARA', 'LOCAL', 'DRAFT', '2026-10-10 10:00:00+02', '2026-10-10 14:00:00+02', '2026-10-05 23:59:59+02')
ON CONFLICT (id) DO UPDATE 
SET name = EXCLUDED.name,
    location = EXCLUDED.location,
    type = EXCLUDED.type,
    status = EXCLUDED.status,
    event_start_date = EXCLUDED.event_start_date,
    event_end_time = EXCLUDED.event_end_time,
    registration_end_date = EXCLUDED.registration_end_date,
    updated_at = CURRENT_TIMESTAMP;

-- ==========================================
-- SEED EVENT DETAILS
-- ==========================================
INSERT INTO public.event_details (id, event_id, description, food_provided, qr_code_content, event_code) VALUES 
('b1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', 'Conferinta interna pentru departamentul tehnic.', true, 'QR_TECH_DAYS', 'TCH001'),
('b2222222-2222-2222-2222-222222222222', 'a2222222-2222-2222-2222-222222222222', 'Workshop dedicat strategiilor de marketing B2B.', false, 'QR_MKT_WS', 'MKT002')
ON CONFLICT (id) DO UPDATE 
SET description = EXCLUDED.description,
    food_provided = EXCLUDED.food_provided,
    qr_code_content = EXCLUDED.qr_code_content,
    event_code = EXCLUDED.event_code;