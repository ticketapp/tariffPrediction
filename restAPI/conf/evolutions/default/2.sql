# --- !Ups
ALTER TABLE places ADD COLUMN last_update TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp NOT NULL;
ALTER TABLE places ALTER COLUMN facebookUrl SET NOT NULL;
ALTER TABLE places RENAME linkedOrganizerId TO linkedOrganizerUrl;


# --- !Downs
ALTER TABLE places DROP COLUMN last_update;
ALTER TABLE places ALTER facebookUrl DROP NOT NULL;
ALTER TABLE places RENAME linkedOrganizerUrl TO linkedOrganizerId;
