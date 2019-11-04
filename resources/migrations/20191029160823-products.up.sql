create table "products"
(
  id        serial,
  name      varchar(200) not null unique,
  photo     varchar      not null default '',
  energy    int          not null default 0,
  price     int          not null default 0,
  thumbnail varchar      not null default '',

  primary key (id)
)