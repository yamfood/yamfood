alter table products
    add payload jsonb NOT NULL default '{}';
