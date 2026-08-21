ALTER TABLE public.events
    ALTER COLUMN event_start_date SET NOT NULL,
    ALTER COLUMN event_end_time SET NOT NULL;

ALTER TABLE public.events
    ADD CONSTRAINT events_end_after_start
        CHECK (event_end_time > event_start_date);

ALTER TABLE public.events
    ADD CONSTRAINT events_reg_end_before_start
        CHECK (registration_end_date IS NULL OR registration_end_date < event_start_date);
