alter table orders
    alter column created_at type timestamp with time zone using created_at::timestamp with time zone,
    alter column created_at set default current_timestamp;