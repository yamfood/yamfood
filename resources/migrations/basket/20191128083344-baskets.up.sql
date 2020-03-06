create table "baskets"
(
  id      serial,
  client_id int not null unique references clients (id),

  primary key (id)
);
