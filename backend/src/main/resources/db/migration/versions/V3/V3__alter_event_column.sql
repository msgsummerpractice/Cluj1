ALTER TABLE public.event_details
ALTER COLUMN poster TYPE bytea
USING NULL::bytea;