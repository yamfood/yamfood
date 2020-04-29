alter table clients
    add constraint clients_phone_key unique (phone),
    drop constraint clients_phone_bot_id_key;