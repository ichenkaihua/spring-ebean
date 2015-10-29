create table user (
  id                            integer not null,
  name                          varchar(255),
  password                      varchar(255),
  constraint pk_user primary key (id)
);
create sequence user_seq;

