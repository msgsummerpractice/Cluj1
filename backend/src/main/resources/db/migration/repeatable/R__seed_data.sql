-- ==========================================
-- SEED USERS
-- ==========================================
INSERT INTO public.users (id, is_active, created_at, updated_at, role, email, password_hash) VALUES 
('11111111-1111-1111-1111-111111111111', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ADMIN', 'admin.suprem@msg.group', '$2a$10$m1HHVgftFkUMhOzDVKPXqOuME7vKUvJp14lFMQBE5O0kxfkd/ByKu'),
('22222222-2222-2222-2222-222222222222', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'MARKETING_ORGANIZER', 'maria.marketing@msg.group', '$2a$10$m1HHVgftFkUMhOzDVKPXqOuME7vKUvJp14lFMQBE5O0kxfkd/ByKu'),
('33333333-3333-3333-3333-333333333333', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'HR_USER', 'elena.hr@msg.group', '$2a$10$m1HHVgftFkUMhOzDVKPXqOuME7vKUvJp14lFMQBE5O0kxfkd/ByKu'),
('44444444-4444-4444-4444-444444444444', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PARTICIPANT', 'andrei.popescu@msg.group', '$2a$10$m1HHVgftFkUMhOzDVKPXqOuME7vKUvJp14lFMQBE5O0kxfkd/ByKu'),
('55555555-5555-5555-5555-555555555555', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PARTICIPANT', 'mihai.ionescu@msg.group', '$2a$10$m1HHVgftFkUMhOzDVKPXqOuME7vKUvJp14lFMQBE5O0kxfkd/ByKu'),
('66666666-6666-6666-6666-666666666666', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PARTICIPANT', 'fost.angajat@msg.group', '$2a$10$m1HHVgftFkUMhOzDVKPXqOuME7vKUvJp14lFMQBE5O0kxfkd/ByKu')
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
-- SEED EVENTS
-- ==========================================
INSERT INTO public.events (id, created_at, updated_at, created_by, name, location, type, status, event_start_date, event_end_time, registration_end_date) VALUES 
('a1111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '11111111-1111-1111-1111-111111111111', 'MSG Tech Days 2026', 'CLUJ', 'INTERNAL', 'PUBLISHED', '2026-09-15 09:00:00+02', '2026-09-16 18:00:00+02', '2026-09-01 23:59:59+02'),
('a2222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '22222222-2222-2222-2222-222222222222', 'Marketing Workshop', 'TIMISOARA', 'LOCAL', 'DRAFT', '2026-10-10 10:00:00+02', '2026-10-10 14:00:00+02', '2026-10-05 23:59:59+02'),
('a3333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '33333333-3333-3333-3333-333333333333', 'IT Job Fair 2026', 'MURES', 'EXTERNAL', 'PUBLISHED', '2026-11-20 09:00:00+02', '2026-11-21 17:00:00+02', '2026-11-18 23:59:59+02'),
('a4444444-4444-4444-4444-444444444444', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '33333333-3333-3333-3333-333333333333', 'HR Onboarding Session', 'ALL', 'INTERNAL', 'PUBLISHED', '2026-08-25 10:00:00+02', '2026-08-25 12:00:00+02', '2026-08-24 18:00:00+02'),
('a5555555-5555-5555-5555-555555555555', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '11111111-1111-1111-1111-111111111111', 'Legacy System Retirement Party', 'CLUJ', 'LOCAL', 'COMPLETED', '2026-01-15 18:00:00+02', '2026-01-15 23:00:00+02', '2026-01-10 23:59:59+02')
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
('b1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', 'Conferinta interna pentru departamentul tehnic.', true, 'a1111111-1111-1111-1111-111111111111', 'TCH001'),
('b2222222-2222-2222-2222-222222222222', 'a2222222-2222-2222-2222-222222222222', 'Workshop dedicat strategiilor de marketing B2B.', false, 'a2222222-2222-2222-2222-222222222222', 'MKT002'),
('b3333333-3333-3333-3333-333333333333', 'a3333333-3333-3333-3333-333333333333', 'Prezentare companie la targul de joburi organizat la nivel regional.', false, 'a3333333-3333-3333-3333-333333333333', 'JOB003'),
('b4444444-4444-4444-4444-444444444444', 'a4444444-4444-4444-4444-444444444444', 'Sesiune de onboarding pentru toti angajatii noi din acest trimestru.', true, 'a4444444-4444-4444-4444-444444444444', 'HR0004'),
('b5555555-5555-5555-5555-555555555555', 'a5555555-5555-5555-5555-555555555555', 'Eveniment intern pentru sarbatorirea inchiderii sistemului legacy.', true, 'a5555555-5555-5555-5555-555555555555', 'LGC005')
ON CONFLICT (id) DO UPDATE 
SET description = EXCLUDED.description,
    food_provided = EXCLUDED.food_provided,
    qr_code_content = EXCLUDED.qr_code_content,
    event_code = EXCLUDED.event_code;

    -- ==========================================
-- SEED REGISTRATIONS (Required for Check-in)
-- ==========================================
INSERT INTO public.registrations (
    id, 
    user_id, 
    event_id, 
    registration_date, 
    accommodation_needed, 
    gdpr_consent, 
    photo_consent, 
    transportation_needed
) VALUES 
-- Register Andrei for a VALID future event (Tech Days)
(
    'c1111111-1111-1111-1111-111111111111', 
    '44444444-4444-4444-4444-444444444444', 
    'a1111111-1111-1111-1111-111111111111', 
    CURRENT_TIMESTAMP, 
    false, 
    true, 
    true, 
    false
),
-- Register Andrei for an INVALID expired/completed event (Legacy Party)
(
    'c5555555-5555-5555-5555-555555555555', 
    '44444444-4444-4444-4444-444444444444', 
    'a5555555-5555-5555-5555-555555555555', 
    CURRENT_TIMESTAMP, 
    false, 
    true, 
    true, 
    false
)
ON CONFLICT (id) DO NOTHING;