create table "orders"
(
    id        serial,
    location  Point not null,
    comment   varchar(300),

    client_id int references clients (id),

    primary key (id)
)