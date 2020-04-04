create table announcements
(
    id        serial,
    image_url varchar(200) not null,
    text      text         not null,

    send_at   timestamp    not null default (now() at time zone 'utc'),
    status    varchar(200) not null,

    primary key (id)
);
