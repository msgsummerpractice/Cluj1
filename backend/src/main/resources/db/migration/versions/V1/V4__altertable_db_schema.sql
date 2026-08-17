-- Setează valoarea implicită (default) pe false pentru coloana food_provided
ALTER TABLE event_details ALTER COLUMN food_provided SET DEFAULT false;

-- Actualizează toate înregistrările existente care au în acest moment valoarea NULL
UPDATE event_details SET food_provided = false WHERE food_provided IS NULL;

-- Scoate constrângerea de NOT NULL ca să permită valori implicite fără erori
ALTER TABLE event_details ALTER COLUMN food_provided DROP NOT NULL;