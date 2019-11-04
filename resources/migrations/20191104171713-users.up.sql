create table "users"
(
  id    serial,
  tid   bigint not null unique,
  phone bigint not null unique,

  primary key (id)
)