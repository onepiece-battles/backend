alter table fruit add column attack_slope double not null;
alter table fruit add column defense_slope double not null;
alter table fruit add column speed_slope double not null;
alter table fruit add column intelligence_slope double not null;
alter table fruit add column group_gain_slope double not null;
alter table fruit add column blood_slope double not null;
alter table fruit add column attack_intercept double not null;
alter table fruit add column defense_intercept double not null;
alter table fruit add column speed_intercept double not null;
alter table fruit add column intelligence_intercept double not null;
alter table fruit add column group_gain_intercept double not null;
alter table fruit add column blood_intercept double not null;
alter table fruit drop column attack_strengthen;
alter table fruit drop column defense_strengthen;
alter table fruit drop column blood_strengthen;
alter table fruit drop column group_gain_strengthen;
alter table fruit drop column speed_strengthen;
alter table fruit drop column intelligence_strengthen;
alter table fruit drop column frui;


