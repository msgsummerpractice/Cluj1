CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

CREATE TABLE public.attendance_records (
    check_in_time timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    registration_id uuid NOT NULL,
    check_in_method character varying(50) NOT NULL,
    CONSTRAINT attendance_records_check_in_method_check CHECK (((check_in_method)::text = ANY ((ARRAY['QR'::character varying, 'MANUAL'::character varying])::text[])))
);

CREATE TABLE public.event_details (
    food_provided boolean NOT NULL,
    event_code character varying(6),
    event_id uuid NOT NULL,
    id uuid NOT NULL,
    description text,
    qr_code_content text,
    poster oid
);

CREATE TABLE public.events (
    created_at timestamp(6) with time zone NOT NULL,
    event_end_time timestamp(6) with time zone,
    event_start_date timestamp(6) with time zone,
    registration_end_date timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid NOT NULL,
    id uuid NOT NULL,
    location character varying(50) NOT NULL,
    status character varying(50) NOT NULL,
    type character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    CONSTRAINT events_location_check CHECK (((location)::text = ANY ((ARRAY['ALL'::character varying, 'CLUJ'::character varying, 'TIMISOARA'::character varying, 'MURES'::character varying])::text[]))),
    CONSTRAINT events_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PUBLISHED'::character varying, 'COMPLETED'::character varying])::text[]))),
    CONSTRAINT events_type_check CHECK (((type)::text = ANY ((ARRAY['INTERNAL'::character varying, 'EXTERNAL'::character varying, 'LOCAL'::character varying])::text[])))
);

CREATE TABLE public.notification_recipients (
    id uuid NOT NULL,
    notification_id uuid NOT NULL,
    user_id uuid NOT NULL
);

CREATE TABLE public.notifications (
    sent_at timestamp(6) with time zone NOT NULL,
    event_id uuid NOT NULL,
    id uuid NOT NULL,
    message text NOT NULL
);

CREATE TABLE public.password_reset_tokens (
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    used_at timestamp(6) with time zone,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    token_hash character varying(255) NOT NULL
);

CREATE TABLE public.registrations (
    accommodation_days integer,
    accommodation_needed boolean NOT NULL,
    gdpr_consent boolean NOT NULL,
    photo_consent boolean NOT NULL,
    transportation_needed boolean NOT NULL,
    registration_date timestamp(6) with time zone NOT NULL,
    event_id uuid NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    food_preference character varying(50),
    CONSTRAINT registrations_food_preference_check CHECK (((food_preference)::text = ANY ((ARRAY['NONE'::character varying, 'VEGETARIAN'::character varying, 'VEGAN'::character varying])::text[])))
);

CREATE TABLE public.spring_session (
    primary_id character(36) NOT NULL,
    session_id character(36) NOT NULL,
    creation_time bigint NOT NULL,
    last_access_time bigint NOT NULL,
    max_inactive_interval integer NOT NULL,
    expiry_time bigint NOT NULL,
    principal_name character varying(100)
);

CREATE TABLE public.spring_session_attributes (
    session_primary_id character(36) NOT NULL,
    attribute_name character varying(200) NOT NULL,
    attribute_bytes bytea NOT NULL
);

CREATE TABLE public.transportation_details (
    id uuid NOT NULL,
    registration_id uuid NOT NULL,
    driver_phone_number character varying(50) NOT NULL,
    driver_name character varying(255) NOT NULL
);

CREATE TABLE public.user_details (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    location character varying(50) NOT NULL,
    first_name character varying(100) NOT NULL,
    last_name character varying(100) NOT NULL,
    profile_picture oid,
    CONSTRAINT user_details_location_check CHECK (((location)::text = ANY ((ARRAY['CLUJ'::character varying, 'TIMISOARA'::character varying, 'MURES'::character varying, 'REMOTE'::character varying])::text[])))
);

CREATE TABLE public.users (
    is_active boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    role character varying(50) NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['PARTICIPANT'::character varying, 'MARKETING_ORGANIZER'::character varying, 'HR_USER'::character varying, 'ADMIN'::character varying])::text[])))
);

ALTER TABLE ONLY public.attendance_records ADD CONSTRAINT attendance_records_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.attendance_records ADD CONSTRAINT attendance_records_registration_id_key UNIQUE (registration_id);
ALTER TABLE ONLY public.event_details ADD CONSTRAINT event_details_event_code_key UNIQUE (event_code);
ALTER TABLE ONLY public.event_details ADD CONSTRAINT event_details_event_id_key UNIQUE (event_id);
ALTER TABLE ONLY public.event_details ADD CONSTRAINT event_details_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.events ADD CONSTRAINT events_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.notification_recipients ADD CONSTRAINT notification_recipients_notification_id_user_id_key UNIQUE (notification_id, user_id);
ALTER TABLE ONLY public.notification_recipients ADD CONSTRAINT notification_recipients_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.notifications ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.password_reset_tokens ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.password_reset_tokens ADD CONSTRAINT password_reset_tokens_token_hash_key UNIQUE (token_hash);
ALTER TABLE ONLY public.password_reset_tokens ADD CONSTRAINT password_reset_tokens_user_id_key UNIQUE (user_id);
ALTER TABLE ONLY public.registrations ADD CONSTRAINT registrations_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.registrations ADD CONSTRAINT registrations_user_id_event_id_key UNIQUE (user_id, event_id);
ALTER TABLE ONLY public.spring_session_attributes ADD CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name);
ALTER TABLE ONLY public.spring_session ADD CONSTRAINT spring_session_pk PRIMARY KEY (primary_id);
ALTER TABLE ONLY public.transportation_details ADD CONSTRAINT transportation_details_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.transportation_details ADD CONSTRAINT transportation_details_registration_id_key UNIQUE (registration_id);
ALTER TABLE ONLY public.user_details ADD CONSTRAINT user_details_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.user_details ADD CONSTRAINT user_details_user_id_key UNIQUE (user_id);
ALTER TABLE ONLY public.users ADD CONSTRAINT users_email_key UNIQUE (email);
ALTER TABLE ONLY public.users ADD CONSTRAINT users_pkey PRIMARY KEY (id);

CREATE UNIQUE INDEX spring_session_ix1 ON public.spring_session USING btree (session_id);
CREATE INDEX spring_session_ix2 ON public.spring_session USING btree (expiry_time);
CREATE INDEX spring_session_ix3 ON public.spring_session USING btree (principal_name);

ALTER TABLE ONLY public.transportation_details ADD CONSTRAINT fk3xxyyh3505rfu5j21qnvq3o4i FOREIGN KEY (registration_id) REFERENCES public.registrations(id);
ALTER TABLE ONLY public.event_details ADD CONSTRAINT fk47othn79xry1mqxc3rvmvr3a FOREIGN KEY (event_id) REFERENCES public.events(id);
ALTER TABLE ONLY public.attendance_records ADD CONSTRAINT fk6edtg8syn5dcgfrxhcjg7ut61 FOREIGN KEY (registration_id) REFERENCES public.registrations(id);
ALTER TABLE ONLY public.registrations ADD CONSTRAINT fk8mi58jt1s8fxmi56jnau0cxqw FOREIGN KEY (event_id) REFERENCES public.events(id);
ALTER TABLE ONLY public.notification_recipients ADD CONSTRAINT fkce9mdpy7u99n8tn3s2iflds4t FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.user_details ADD CONSTRAINT fkicouhgavvmiiohc28mgk0kuj5 FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.notification_recipients ADD CONSTRAINT fkiuf5qgbttjq6ry57u1dni7qn4 FOREIGN KEY (notification_id) REFERENCES public.notifications(id);
ALTER TABLE ONLY public.password_reset_tokens ADD CONSTRAINT fkk3ndxg5xp6v7wd4gjyusp15gq FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.notifications ADD CONSTRAINT fkkau4auenkuj1m86s9mydmets FOREIGN KEY (event_id) REFERENCES public.events(id);
ALTER TABLE ONLY public.registrations ADD CONSTRAINT fkl2iby9n9hp8jwkfj8i96pkxpi FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.events ADD CONSTRAINT fkmpv90a1lsx9lcxsj7xjcvvsxg FOREIGN KEY (created_by) REFERENCES public.users(id);
ALTER TABLE ONLY public.spring_session_attributes ADD CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id) REFERENCES public.spring_session(primary_id) ON DELETE CASCADE;