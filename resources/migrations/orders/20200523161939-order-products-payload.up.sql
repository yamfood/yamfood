alter table order_products
    add payload jsonb NOT NULL default '{}';
