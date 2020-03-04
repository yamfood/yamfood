create table disabled_products
(
    id         serial not null,
    product_id int    not null references products (id),
    kitchen_id int    not null references kitchens (id),

    primary key (id)
)