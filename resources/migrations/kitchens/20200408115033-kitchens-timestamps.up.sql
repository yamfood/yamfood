alter table kitchens
    alter column start_at type time with time zone using start_at::time with time zone,
    alter column start_at drop default,
    alter column end_at type time with time zone using end_at::time with time zone,
    alter column end_at drop default;
