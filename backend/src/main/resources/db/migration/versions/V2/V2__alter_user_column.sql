ALTER TABLE public.user_details
ALTER COLUMN profile_picture TYPE bytea
USING NULL::bytea;