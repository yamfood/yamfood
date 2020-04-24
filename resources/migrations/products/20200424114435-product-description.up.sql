alter table products
    add column description jsonb not null default '{}';