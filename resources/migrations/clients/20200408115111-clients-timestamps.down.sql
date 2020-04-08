alter table clients
    alter column created_at type timestamp using created_at::timestamp,
    alter column created_at set default (now() at time zone 'utc');