create table "order_products"
(
  id         serial,
  order_id   int not null references orders (id),
  product_id int not null references products (id),
  count      int not null,

  unique (order_id, product_id),
  primary key (id)
)