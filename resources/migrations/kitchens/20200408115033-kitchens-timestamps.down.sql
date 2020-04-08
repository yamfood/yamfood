alter table kitchens
    alter column start_at type time using start_at::time,
    alter column start_at set default '08:00:00',
    alter column end_at type time using end_at::time,
    alter column end_at set default '23:00:00';
