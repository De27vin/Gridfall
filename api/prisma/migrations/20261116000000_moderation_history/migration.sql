CREATE TABLE "CommunityModerationEvent" (
 "id" UUID PRIMARY KEY, "mapId" UUID NOT NULL REFERENCES "CommunityMap"("id") ON DELETE RESTRICT,
 "moderatorId" UUID NOT NULL REFERENCES "User"("id") ON DELETE RESTRICT,
 "action" VARCHAR(6) NOT NULL CHECK ("action" IN ('hide','remove')), "reason" VARCHAR(500) NOT NULL,
 "previousState" JSONB NOT NULL, "resultingState" JSONB NOT NULL,
 "createdAt" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX "CommunityModerationEvent_mapId_createdAt_id_idx" ON "CommunityModerationEvent" ("mapId","createdAt","id");
CREATE FUNCTION reject_moderation_history_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Moderation history is immutable'; END; $$;
CREATE TRIGGER immutable_moderation_history BEFORE UPDATE OR DELETE ON "CommunityModerationEvent"
FOR EACH ROW EXECUTE FUNCTION reject_moderation_history_mutation();
