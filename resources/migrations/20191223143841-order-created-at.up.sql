alter table orders
  add created_at timestamp default (now() at time zone 'utc') not null;

