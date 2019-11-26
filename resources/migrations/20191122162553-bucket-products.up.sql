create table "bucket_products"
(
  id         serial,
  bucket_id  int not null references buckets (id),
  product_id int not null references products (id),
  count      int default 1,

  unique (bucket_id, product_id),
  primary key (id)
)

