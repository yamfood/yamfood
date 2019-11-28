create table "basket_products"
(
  id         serial,
  basket_id  int not null references baskets (id),
  product_id int not null references products (id),
  count      int default 1,

  unique (basket_id, product_id),
  primary key (id)
)

