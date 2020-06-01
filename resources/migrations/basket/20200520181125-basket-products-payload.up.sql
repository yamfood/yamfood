alter table basket_products
    add payload jsonb NOT NULL default '{}';
