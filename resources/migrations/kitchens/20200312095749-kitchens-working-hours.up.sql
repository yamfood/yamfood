alter table kitchens
    add start_at time not null default '08:00:00',
    add end_at   time not null default '23:00:00';