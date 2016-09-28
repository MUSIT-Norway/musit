# --- !Ups


insert into MUSARK_STORAGE.STORAGE_NODE(storage_node_id) values(3);

insert into MUSIT_MAPPING.VIEW_MUSITTHING(id, displayId, displayName) values(1, 'C666/34', 'Øks');
insert into MUSIT_MAPPING.VIEW_MUSITTHING(id, displayId, displayName) values(2, 'C666/31', 'Sverd');
insert into MUSIT_MAPPING.VIEW_MUSITTHING(id, displayId, displayName) values(3, 'C666/38', 'Sommerfugl');

insert into MUSARK_STORAGE.LOCAL_OBJECT(object_id, latest_move_id, current_location_id) values(1, 23, 3);
insert into MUSARK_STORAGE.LOCAL_OBJECT(object_id, latest_move_id, current_location_id) values(2, 23, 3);
insert into MUSARK_STORAGE.LOCAL_OBJECT(object_id, latest_move_id, current_location_id) values(3, 23, 3);
