alter table modifiers
    add group_id uuid not null default uuid_generate_v4();
