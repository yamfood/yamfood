create table "baskets"
(
  id      serial,
  user_id int not null unique references users (id),

  primary key (id)
);
