alter table riders
    add is_blocked bool default false not null,
    add notes      text default ''    not null;