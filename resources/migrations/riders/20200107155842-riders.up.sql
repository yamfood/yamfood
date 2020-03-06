create table "riders"
(
  id    serial,
  tid   bigint       not null unique,
  phone bigint       not null unique,
  name  varchar(200) not null,

  primary key (id)
)