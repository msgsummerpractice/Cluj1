ALTER TABLE event_details ALTER COLUMN food_provided SET DEFAULT false;

UPDATE event_details SET food_provided = false WHERE food_provided IS NULL;

ALTER TABLE event_details ALTER COLUMN food_provided DROP NOT NULL;