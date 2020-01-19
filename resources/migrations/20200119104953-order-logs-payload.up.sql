alter table order_logs
  add payload json NOT NULL default '{}';
