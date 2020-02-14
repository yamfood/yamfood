alter table orders
    add kitchen_id int references kitchens (id);
