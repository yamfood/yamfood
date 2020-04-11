alter table announcements
    alter column send_at type timestamp with time zone using send_at::timestamp with time zone,
    alter column send_at set default current_timestamp;