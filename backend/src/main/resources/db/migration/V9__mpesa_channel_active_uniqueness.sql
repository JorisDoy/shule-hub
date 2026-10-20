-- A branch may re-add the same paybill (for example to rotate credentials): only one active
-- channel per shortcode is required, and deactivated ones stay for history.
alter table mpesa_channel drop constraint uq_mpesa_channel;
create unique index ux_mpesa_channel_active on mpesa_channel (tenant_id, branch_id, shortcode) where active;
