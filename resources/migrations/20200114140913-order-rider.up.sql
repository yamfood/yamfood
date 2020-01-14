alter table orders
  add rider_id int references riders (id) default null;

