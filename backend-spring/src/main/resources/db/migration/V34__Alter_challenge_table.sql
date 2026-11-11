-- max_point, min_point, decrease_point를 넣고 기존 points의 값을 max_point로 옮긴다.
alter table challenges
	add column max_point integer not null default 0;

alter table challenges
	add column min_point integer not null default 0;
    
alter table challenges
	add column decrease_point integer not null default 0;

UPDATE challenges SET max_point = points;

alter table challenges
    drop column points;