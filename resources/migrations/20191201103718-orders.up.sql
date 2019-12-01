create table "orders"
(
  id       serial,
  location Point not null,
  comment  varchar(300),

  user_id  int references users (id),

  primary key (id)
)