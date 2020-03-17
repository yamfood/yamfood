alter table clients
    add created_at timestamp default (now() at time zone 'utc') not null;

