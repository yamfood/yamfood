create table "order_logs"
(
  id         serial,
  order_id   int          not null references orders (id),
  status     varchar(200) not null,
  created_at timestamp    not null default (now() at time zone 'utc'),

  primary key (id)
)