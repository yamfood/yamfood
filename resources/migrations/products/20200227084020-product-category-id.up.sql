alter table products
    add category_id int references categories (id);