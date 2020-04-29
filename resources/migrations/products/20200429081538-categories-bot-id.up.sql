alter table categories
    add bot_id int references bots (id);
