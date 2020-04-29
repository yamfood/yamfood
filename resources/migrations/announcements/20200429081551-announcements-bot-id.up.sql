alter table announcements
    add bot_id int references bots (id);
