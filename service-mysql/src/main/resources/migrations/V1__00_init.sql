create table sql_feature_state (
  name                          varchar(255) not null primary key,
  when_enabled                  datetime,
  feature_locked                tinyint(1) default 0 not null
);

