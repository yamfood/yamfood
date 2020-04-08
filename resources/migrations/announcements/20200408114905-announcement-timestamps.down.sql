alter table announcements
    alter column send_at type timestamp,
    alter column send_at set default (now() at time zone 'utc');