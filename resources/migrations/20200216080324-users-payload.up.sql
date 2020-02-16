alter table users
    add payload jsonb NOT NULL default '{}';
