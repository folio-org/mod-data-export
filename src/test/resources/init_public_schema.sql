
create table public.uuid_range
(
    id             serial primary key,
    partition      uuid,
    subrange_start uuid,
    subrange_end   uuid,
    completed      boolean default false
);

DO
$$
    DECLARE
        partition_start UUID;
        subrange_start  UUID;
        subrange_end    UUID;
    BEGIN
        FOR i IN 0..15
            LOOP
                partition_start := (rpad(to_hex(i), 8, '0') || '-0000-0000-0000-000000000000')::UUID;
                subrange_start := partition_start;

                FOR j IN 1..4096
                    LOOP
                        IF i < 15 OR (i = 15 AND j < 4096) THEN
                            if (j < 4096) then
                                subrange_end := (to_hex(i)::text || rpad(lpad(to_hex(j), 3, '0'), 7, '0') ||
                                                 '-0000-0000-0000-000000000000')::UUID;
                            else
                                subrange_end := (rpad(to_hex(i + 1), 8, '0') || '-0000-0000-0000-000000000000')::UUID;
                            end if;
                        ELSE
                            subrange_end := 'ffffffff-ffff-ffff-ffff-ffffffffffff'::UUID; -- upper bound for last subrange in last partition
                        END IF;

                        INSERT INTO public.uuid_range (partition, subrange_start, subrange_end)
                        VALUES (partition_start, subrange_start, subrange_end);

                        subrange_start := subrange_end;
                    END LOOP;
            END LOOP;
    END;
$$
LANGUAGE plpgsql;