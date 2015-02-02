#! /bin/sh
mysqld --datadir=/var/lib/mysql --user=mysql --init-file /initdb/initdb.sql &
sleep 3
mysqladmin -u root shutdown
