select t1.id,t1.str,t1.create_time,t2.type_name from test t1 left join types t2 on t1.type_id = t2.id
