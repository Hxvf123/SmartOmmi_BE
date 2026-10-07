-- Pre-JWT authentication gets access only to the account being authenticated.
-- These transaction-local settings are set exclusively inside AuthService.
CREATE POLICY authentication_account ON users
  USING (
    email = NULLIF(current_setting('app.auth_email', true), '')
    OR id = NULLIF(current_setting('app.auth_user_id', true), '')::bigint
  )
  WITH CHECK (
    email = NULLIF(current_setting('app.auth_email', true), '')
    OR id = NULLIF(current_setting('app.auth_user_id', true), '')::bigint
  );

CREATE POLICY authentication_reset_tokens ON password_reset_tokens
  USING (EXISTS (
    SELECT 1 FROM users u WHERE u.id = password_reset_tokens.user_id
    AND (u.email = NULLIF(current_setting('app.auth_email', true), '')
         OR u.id = NULLIF(current_setting('app.auth_user_id', true), '')::bigint)
  ))
  WITH CHECK (EXISTS (
    SELECT 1 FROM users u WHERE u.id = password_reset_tokens.user_id
    AND (u.email = NULLIF(current_setting('app.auth_email', true), '')
         OR u.id = NULLIF(current_setting('app.auth_user_id', true), '')::bigint)
  ));

CREATE POLICY password_reset_lookup ON password_reset_tokens
  FOR SELECT USING (
    token = NULLIF(current_setting('app.reset_token', true), '')
    AND NOT used AND expires_at > now()
  );
CREATE POLICY password_reset_consume ON password_reset_tokens
  FOR UPDATE USING (
    token = NULLIF(current_setting('app.reset_token', true), '')
    AND NOT used AND expires_at > now()
  ) WITH CHECK (
    token = NULLIF(current_setting('app.reset_token', true), '')
  );

-- Explicit background capabilities, without granting super_admin across the schema.
CREATE POLICY outbox_worker_read ON inventory_outbox_events
  FOR SELECT USING (app_current_role() = 'outbox_worker');
CREATE POLICY outbox_worker_update ON inventory_outbox_events
  FOR UPDATE USING (app_current_role() = 'outbox_worker')
  WITH CHECK (app_current_role() = 'outbox_worker');
CREATE POLICY price_sync_worker_read ON marketplace_connections
  FOR SELECT USING (app_current_role() = 'price_sync_worker');

-- Do not expose migration metadata to the CRUD role, including after legacy bootstrap.
REVOKE ALL ON TABLE flyway_schema_history FROM smartomni_app;
