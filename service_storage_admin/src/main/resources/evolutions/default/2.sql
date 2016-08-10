# --- !Ups

insert into MUSARK_STORAGE.STORAGE_UNIT (
	storage_unit_id,
    storage_unit_name,
    area,
    area_to,
    is_storage_unit,
    is_part_of,
    height,
    height_to,
    storage_type,
    group_read,
    group_write
) values (
	1,
	'B1',
	1,
	1,
	1,
	null,
	1,
	1,
	'Building',
	'foo',
	'bar'
);

insert into MUSARK_STORAGE.BUILDING (
	storage_unit_id,
	postal_address
) values (
    1,
	'Foo street 5, 3427 Bar'
);

insert into MUSARK_STORAGE.STORAGE_UNIT (
	storage_unit_id,
    storage_unit_name,
    area,
    area_to,
    is_storage_unit,
    is_part_of,
    height,
    height_to,
    storage_type,
    group_read,
    group_write
) values (
	2,
	'R1',
	1,
	1,
	1,
	1,
	1,
	1,
	'Room',
	'foo',
	'bar'
);

insert into MUSARK_STORAGE.ROOM (
 storage_unit_id,
 sikring_skallsikring,
 sikring_tyverisikring,
 sikring_brannsikring,
 sikring_vannskaderisiko,
 sikring_rutine_og_beredskap,
 bevar_luftfukt_og_temp,
 bevar_lysforhold,
 bevar_prevant_kons
) values (
	2,
	1,
	1,
	1,
	1,
	1,
	1,
	1,
	1
);

insert into MUSARK_STORAGE.STORAGE_UNIT (
	storage_unit_id,
    storage_unit_name,
    area,
    area_to,
    is_storage_unit,
    is_part_of,
    height,
    height_to,
    storage_type,
    group_read,
    group_write
) values (
	3,
	'SU1',
	1,
	1,
	1,
	2,
	1,
	1,
	'StorageUnit',
	'foo',
	'bar'
);

insert into MUSARK_STORAGE.STORAGE_UNIT (
	storage_unit_id,
    storage_unit_name,
    area,
    area_to,
    is_storage_unit,
    is_part_of,
    height,
    height_to,
    storage_type,
    group_read,
    group_write
) values (
	4,
	'SU2',
	1,
	1,
	1,
	2,
	1,
	1,
	'StorageUnit',
	'foo',
	'bar'
);

# --- !Downs
